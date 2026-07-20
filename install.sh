#!/bin/bash

# Sjekk root
if [[ $EUID -ne 0 ]]; then
    echo "Dette scriptet må kjøres som root (sudo)"
    exit 1
fi

TARGET_USER="kammich"
USER_ID=""
GROUP_ID=""
MOUNT_ROOT="/run/kammich/removable"
STATE_ROOT="/run/kammich"

###############################################
# 0. Opprett dedikert bruker for tjeneste
###############################################
setup_kammich_user() {
    echo "--- Sjekker/oppretter dedikert bruker ($TARGET_USER) ---"

    if ! id "$TARGET_USER" &>/dev/null; then
        adduser --disabled-password --gecos "" "$TARGET_USER"
        echo "Brukeren $TARGET_USER ble opprettet som normal bruker."
    else
        echo "Brukeren $TARGET_USER eksisterer allerede."
    fi

    # Opprett hjemmemappe og rettigheter
    mkdir -p /home/$TARGET_USER
    chown -R "$TARGET_USER:$TARGET_USER" /home/$TARGET_USER
    chmod 755 /home/$TARGET_USER

    # Gi brukeren tilgang til relevante enhetsgrupper
    usermod -aG input,tty,audio "$TARGET_USER"

    USER_ID=$(id -u "$TARGET_USER")
    GROUP_ID=$(id -g "$TARGET_USER")

    echo "Bruker-ID: $USER_ID, Gruppe-ID: $GROUP_ID"
}

###############################################
# 1. Setup Base-struktur (Persistent mount path)
###############################################
setup_paths() {
    mkdir -p "$MOUNT_ROOT"
    echo "MOUNT_ROOT=\"$MOUNT_ROOT\"" > /etc/kammich.conf
    echo "STATE_ROOT=\"$STATE_ROOT\"" >> /etc/kammich.conf

    cat <<EOF > /etc/tmpfiles.d/kammich.conf
d $STATE_ROOT 0755 root root -
EOF
}

###############################################
# 2. Installer dependencies (Uten GUI/Kiosk)
###############################################
install_dependencies() {
    echo "--- Installerer systemavhengigheter ---"

    if command -v apt-get &> /dev/null; then
        echo "--- Ubuntu/Debian-basert system oppdaget ---"
        apt-get update
        apt-get install -y \
            gphoto2 smartmontools hdparm openjdk-21-jdk rfkill jc network-manager

    elif command -v dnf &> /dev/null; then
        echo "--- Fedora/RHEL-basert system oppdaget ---"
        dnf check-update && dnf install -y \
            gphoto2 smartmontools hdparm java-21-openjdk-devel rfkill jc network-manager

    elif command -v pacman &> /dev/null; then
        echo "--- Arch-basert system oppdaget ---"
        pacman -Sy --noconfirm \
            gphoto2 smartmontools hdparm jdk21-openjdk rfkill jc network-manager

    else
        echo "Kunne ikke identifisere pakkebehandler."
        exit 1
    fi
}

###############################################
# 3. Udev-regler
###############################################
configure_udev() {
    echo "--- Konfigurerer universelle udev-regler ---"

    cat <<EOF > /etc/udev/rules.d/99-kammich.rules
ACTION=="add", SUBSYSTEM=="block", ENV{ID_BUS}=="usb", KERNEL=="sd[a-z][0-9]|nvme[0-9]n[0-9]p[0-9]", ENV{SYSTEMD_WANTS}+="usb-mount@%k.service"
ACTION=="add", SUBSYSTEM=="block", SUBSYSTEMS=="usb", KERNEL=="sd[a-z][0-9]", ENV{SYSTEMD_WANTS}+="usb-mount@%k.service"
ACTION=="remove", SUBSYSTEM=="block", KERNEL=="sd[a-z][0-9]|nvme[0-9]n[0-9]p[0-9]", ENV{SYSTEMD_WANTS}+="usb-unmount@%k.service"
EOF
}

###############################################
# 4. Mount/Unmount Helper
###############################################
configure_systemd() {
    echo "--- Konfigurerer systemd services ---"

    cat <<EOF > /usr/local/bin/kammich-mount-helper
#!/bin/bash
source /etc/kammich.conf
ACTION=\$1
DEV=\$2
MODEL_FILE="$STATE_ROOT/\${DEV}.model"

if [ "\$ACTION" == "prepare" ]; then
    DEVPATH=\$(udevadm info -q path -n "/dev/\$DEV")
    PARENTPATH=\$(echo "\$DEVPATH" | sed 's/\/[^\/]*\$//')
    MODEL=\$(udevadm info -q property -p "\$PARENTPATH" | grep "^ID_MODEL=" | cut -d= -f2)
    echo "\${MODEL:-Unknown_Device}" | tr -d " ()/" > "\$MODEL_FILE"

elif [ "\$ACTION" == "mount" ]; then
    MODEL=\$(cat "\$MODEL_FILE" | tr -d '[:space:]')
    MODEL=\${MODEL:-Unknown_Device}
    TARGET="\$MOUNT_ROOT/\$MODEL/\$DEV"

    FSTYPE=\$(blkid -o value -s TYPE "/dev/\$DEV")
    if [ -z "\$FSTYPE" ]; then
        exit 0
    fi

    mkdir -p "\$TARGET"

    if grep -qs "\$TARGET" /proc/mounts; then
        exit 0
    fi

    case "\$FSTYPE" in
      ntfs|ntfs-3g)
        systemd-run --scope --collect --property=Description="Mount \$DEV" mount -t ntfs-3g -o uid=$USER_ID,gid=$GROUP_ID,umask=000 "/dev/\$DEV" "\$TARGET"
        ;;
      vfat|fat|exfat)
        mount -o uid=$USER_ID,gid=$GROUP_ID,umask=000,fmask=000,dmask=000 "/dev/\$DEV" "\$TARGET"
        ;;
      BitLocker|swap|crypto_LUKS)
        exit 0
        ;;
      *)
        mount "/dev/\$DEV" "\$TARGET"
        ;;
    esac
fi
EOF

    chmod +x /usr/local/bin/kammich-mount-helper

    cat <<EOF > /etc/systemd/system/usb-mount@.service
[Unit]
Description=Automount USB device %I
After=systemd-udevd.service

[Service]
Type=oneshot
ExecStartPre=/usr/local/bin/kammich-mount-helper prepare %I
ExecStart=/usr/local/bin/kammich-mount-helper mount %I
EOF

    cat <<'EOF' > /etc/systemd/system/usb-unmount@.service
[Unit]
Description=Auto-unmount USB device %I

[Service]
Type=oneshot
ExecStart=/bin/bash -c '
DISK="%I"
for PART in $(lsblk -ln -o NAME "/dev/${DISK}" | tail -n +2); do
  MODEL_FILE="/run/kammich/${PART}.model"
  [ -f "$MODEL_FILE" ] || continue
  MODEL=$(cat "$MODEL_FILE")
  TARGET="/run/kammich/removable/${MODEL}/${PART}"
  umount "$TARGET" 2>/dev/null
  rm -f "$MODEL_FILE"
done
'
EOF
}

###############################################
# 5. Eject Script + Sudoers
###############################################
configure_eject() {
    echo "--- Konfigurerer eject-verktøy ---"

    cat <<'EOF' > /usr/local/bin/kammich-eject
#!/bin/bash
source /etc/kammich.conf

DEV="$1"
MODEL_FILE="${STATE_ROOT}/${DEV}.model"

if [ ! -f "$MODEL_FILE" ]; then
    echo "Kunne ikke finne modell-fil for $DEV"
    exit 1
fi

MODEL=$(cat "$MODEL_FILE")
TARGET="${MOUNT_ROOT}/${MODEL}/${DEV}"

sync
umount "$TARGET" 2>/dev/null

[[ "$DEV" == sd* ]] && hdparm -Y "/dev/$DEV" 2>/dev/null

USB_PATH=$(readlink -f /sys/block/"$DEV"/device)
[[ -e "$USB_PATH/usb_device/authorized" ]] && echo 0 > "$USB_PATH/usb_device/authorized"

rm -f "$MODEL_FILE"
EOF

    chmod +x /usr/local/bin/kammich-eject
}

configure_sudo() {
    echo "--- Konfigurerer /etc/sudoers.d/kammich ---"

    sudo bash -c 'cat > /etc/sudoers.d/kammich <<EOF
Cmnd_Alias KAMMICH_ADMIN = /usr/sbin/smartctl, /usr/local/bin/kammich-eject
Cmnd_Alias KAMMICH_NETWORK = /usr/bin/systemctl restart hostapd, /usr/bin/systemctl stop hostapd, /usr/bin/systemctl start hostapd, /usr/bin/systemctl status hostapd, /usr/bin/systemctl restart dnsmasq, /usr/sbin/iw, /usr/bin/nmcli, /usr/bin/pkill, /usr/bin/kill, /usr/sbin/ip
kammich ALL=(ALL) NOPASSWD: KAMMICH_ADMIN, KAMMICH_NETWORK
EOF'

    sudo chmod 0440 /etc/sudoers.d/kammich

    if sudo visudo -cf /etc/sudoers.d/kammich; then
        echo "Suksess: sudoers-filen er validert og klar."
    else
        echo "FEIL: Noe gikk galt under validering!"
        return 1
    fi
}

configure_network() {
    echo "net.ipv4.ip_forward=1" | sudo tee /etc/sysctl.d/99-kammich-forwarding.conf
    sudo sysctl -p /etc/sysctl.d/99-kammich-forwarding.conf
}

###############################################
# 6. Reload og trigger
###############################################
apply_changes() {
    echo "--- Reloading daemon and udev ---"
    systemctl daemon-reload
    udevadm control --reload-rules && udevadm trigger

    echo "--- Kammich-grunnmiljøet er nå klargjort! ---"
}

###############################################
# Hoved-flyt
###############################################
setup_kammich_user
install_dependencies
setup_paths
configure_udev
configure_systemd
configure_eject
configure_sudo
configure_network
apply_changes