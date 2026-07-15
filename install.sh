#!/bin/bash

# Sjekk root
if [[ $EUID -ne 0 ]]; then
    echo "Dette scriptet må kjøres som root (sudo)"
    exit 1
fi

TARGET_USER=${SUDO_USER:-$USER}
USER_ID=$(id -u "$TARGET_USER")
GROUP_ID=$(id -g "$TARGET_USER")
MOUNT_ROOT="/run/kammich/removable"
STATE_ROOT="/run/kammich"

###############################################
# 1. Setup Base-struktur (Persistent mount path)
###############################################
setup_paths() {
    mkdir -p "$MOUNT_ROOT"
    # Lag en fil som begge skript kan lese
    echo "MOUNT_ROOT=\"$MOUNT_ROOT\"" > /etc/kammich.conf
    echo "STATE_ROOT=\"$STATE_ROOT\"" >> /etc/kammich.conf

    cat <<EOF > /etc/tmpfiles.d/kammich.conf
d $STATE_ROOT 0755 root root -
EOF
}


###############################################
# 2. Installer dependencies
###############################################
install_dependencies() {
    echo "--- Installerer systemavhengigheter ---"
    if command -v apt-get &> /dev/null; then
        apt-get update && apt-get install -y gphoto2 smartmontools hdparm openjdk-21-jdk hostapd dnsmasq rfkill jc nmcli
    elif command -v dnf &> /dev/null; then
        dnf check-update && dnf install -y gphoto2 smartmontools hdparm java-21-openjdk-devel hostapd dnsmasq rfkill jc nmcli
    elif command -v pacman &> /dev/null; then
        pacman -Sy --noconfirm gphoto2 smartmontools hdparm hdparm jdk21-openjdk hostapd dnsmasq rfkill jc nmcli
    else
        echo "Kunne ikke identifisere pakkebehandler."
        exit 1
    fi
}

###############################################
# 3. Udev-regler (kun trigger, ingen RUN)
###############################################
configure_udev() {
    echo "--- Konfigurerer universelle udev-regler ---"

    cat <<EOF > /etc/udev/rules.d/99-kammich.rules
# 1. Standardregel for korrekte USB-enheter
ACTION=="add", SUBSYSTEM=="block", ENV{ID_BUS}=="usb", KERNEL=="sd[a-z][0-9]|nvme[0-9]n[0-9]p[0-9]", ENV{SYSTEMD_WANTS}+="usb-mount@%k.service"

# 2. Robust regel for SATA-adaptere: Sjekk om det finnes en USB-forelder (SUBSYSTEMS)
ACTION=="add", SUBSYSTEM=="block", SUBSYSTEMS=="usb", KERNEL=="sd[a-z][0-9]", ENV{SYSTEMD_WANTS}+="usb-mount@%k.service"

# Unmount-regel (fungerer på tvers av busstype så lenge partisjonen finnes)
ACTION=="remove", SUBSYSTEM=="block", KERNEL=="sd[a-z][0-9]|nvme[0-9]n[0-9]p[0-9]", ENV{SYSTEMD_WANTS}+="usb-unmount@%k.service"
EOF
}

###############################################
# 4. Mount/Unmount Helper
###############################################
configure_systemd() {
    echo "--- Konfigurerer systemd services ---"

    # 1. Oppdatert Helper-script
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

    # Finn filsystemtype med blkid
    FSTYPE=\$(blkid -o value -s TYPE "/dev/\$DEV")

    # Hvis ingen filsystem-type, hopp over (f.eks. MSR, swap)
    if [ -z "\$FSTYPE" ]; then
        exit 0
    fi

    mkdir -p "\$TARGET"

    if grep -qs "\$TARGET" /proc/mounts; then
        exit 0
    fi

    case "\$FSTYPE" in
      ntfs|ntfs-3g)
        # Bruk systemd-run for å frikoble NTFS/FUSE fra systemd-tjenestens livssyklus
        systemd-run --scope --collect --property=Description="Mount \$DEV" mount -t ntfs-3g -o uid=$USER_ID,gid=$GROUP_ID,umask=000 "/dev/\$DEV" "\$TARGET"
        ;;
      vfat|fat|exfat)
        mount -o uid=$USER_ID,gid=$GROUP_ID,umask=000,fmask=000,dmask=000 "/dev/\$DEV" "\$TARGET"
        ;;
      BitLocker|swap|crypto_LUKS)
        # Ignorer BitLocker-partisjoner stille
        exit 0
        ;;
      *)
        # Forsøk standard mount for alt annet (ext4, btrfs, osv.)
        mount "/dev/\$DEV" "\$TARGET"
        ;;
    esac
fi
EOF



    chmod +x /usr/local/bin/kammich-mount-helper

    # 2. Oppdatert Mount Service
    cat <<EOF > /etc/systemd/system/usb-mount@.service
[Unit]
Description=Automount USB device %I
After=systemd-udevd.service

[Service]
Type=oneshot
ExecStartPre=/usr/local/bin/kammich-mount-helper prepare %I
ExecStart=/usr/local/bin/kammich-mount-helper mount %I
EOF



    # 3. Unmount Service
    cat <<'EOF' > /etc/systemd/system/usb-unmount@.service
[Unit]
Description=Auto-unmount USB device %I

[Service]
Type=oneshot
ExecStart=/bin/bash -c '
DISK="%I"

# Finn alle partisjoner for disken (sdX → sdX1, sdX2 … / nvme0n1 → nvme0n1p1 …)
for PART in $(lsblk -ln -o NAME "/dev/${DISK}" | tail -n +2); do
  MODEL_FILE="/run/kammich/${PART}.model"

  # Hopp over hvis vi ikke har modell-fil
  [ -f "$MODEL_FILE" ] || continue

  MODEL=$(cat "$MODEL_FILE")
  TARGET="/run/kammich/removable/${MODEL}/${PART}"

  # Unmount partisjonen
  umount "$TARGET" 2>/dev/null

  # Slett modell-fil
  rm -f "$MODEL_FILE"
done
'
EOF

}

###############################################
# 4. Eject Script + Sudoers
###############################################
configure_eject() {
    echo "--- Konfigurerer eject-verktøy og sudoers ---"

    # Vi bruker 'EOF' (med apostrof) slik at Bash ikke tolker $ variabler her,
    # men heller skriver dem direkte inn i filen.
    cat <<'EOF' > /usr/local/bin/kammich-eject
#!/bin/bash
# Hent stiene fra felles konfigurasjonsfil
source /etc/kammich.conf

DEV="$1"
MODEL_FILE="${STATE_ROOT}/${DEV}.model"

# Sjekk at vi har en modell-fil
if [ ! -f "$MODEL_FILE" ]; then
    echo "Kunne ikke finne modell-fil for $DEV"
    exit 1
fi

MODEL=$(cat "$MODEL_FILE")
TARGET="${MOUNT_ROOT}/${MODEL}/${DEV}"

sync
umount "$TARGET" 2>/dev/null

# Strømsparing og sikker utmating
[[ "$DEV" == sd* ]] && hdparm -Y "/dev/$DEV" 2>/dev/null

USB_PATH=$(readlink -f /sys/block/"$DEV"/device)
[[ -e "$USB_PATH/usb_device/authorized" ]] && echo 0 > "$USB_PATH/usb_device/authorized"

# Valgfritt: Slett modell-filen når enheten er utmatet
rm -f "$MODEL_FILE"
EOF

    chmod +x /usr/local/bin/kammich-eject
}

###############################################
# 5. Sudoers
###############################################
configure_sudo() {
    echo "--- Konfigurerer /etc/sudoers.d/kammich ---"

    # Vi bruker en 'heredoc' uten innrykk i selve filinnholdet
    # for å garantere at sudoers-parseren ikke klikker.
    sudo bash -c 'cat > /etc/sudoers.d/kammich <<EOF
# Kammich admin og nettverksstyring
Cmnd_Alias KAMMICH_ADMIN = /usr/sbin/smartctl, /usr/local/bin/kammich-eject
Cmnd_Alias KAMMICH_NETWORK = /usr/bin/systemctl restart hostapd, /usr/bin/systemctl stop hostapd, /usr/bin/systemctl start hostapd, /usr/bin/systemctl status hostapd, /usr/bin/systemctl restart dnsmasq, /usr/sbin/iw, /usr/bin/nmcli

%sudo ALL=(ALL) NOPASSWD: KAMMICH_ADMIN, KAMMICH_NETWORK
EOF'

    # Sett korrekte rettigheter (obligatorisk for sudoers)
    sudo chmod 0440 /etc/sudoers.d/kammich

    # Valider med visudo
    if sudo visudo -cf /etc/sudoers.d/kammich; then
        echo "Suksess: sudoers-filen er validert og klar."
    else
        echo "FEIL: Noe gikk galt under validering!"
        # Vi sletter ikke fila automatisk her,
        # da det er bedre å la den ligge for feilsøking
        return 1
    fi
}

###############################################
# 6. Reload og trigger
###############################################
apply_changes() {
    echo "--- Reloading daemon and udev ---"
    systemctl daemon-reload
    udevadm control --reload-rules && udevadm trigger
    echo "--- Kammich-miljøet er nå klargjort ---"
}

###############################################
# Hoved-flyt
###############################################
install_dependencies
setup_paths
configure_udev
configure_systemd
configure_eject
configure_sudo
apply_changes
