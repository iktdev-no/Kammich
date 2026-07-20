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
# 0. Opprett dedikert bruker for Kiosk/Tjeneste
###############################################
setup_kammich_user() {
    echo "--- Sjekker/oppretter dedikert bruker ($TARGET_USER) ---"

    if ! id "$TARGET_USER" &>/dev/null; then
        adduser --disabled-password --gecos "" "$TARGET_USER"
        echo "Brukeren $TARGET_USER ble opprettet som normal bruker."
    else
        echo "Brukeren $TARGET_USER eksisterer allerede."
    fi

    # TVING frem hjemmemappe og riktige rettigheter hver gang skriptet kjører
    mkdir -p /home/$TARGET_USER/.config/chromium
    chown -R "$TARGET_USER:$TARGET_USER" /home/$TARGET_USER
    chmod 755 /home/$TARGET_USER

    # Gi brukeren tilgang til grafikk, input og tty
    usermod -aG video,input,tty,audio "$TARGET_USER"

    USER_ID=$(id -u "$TARGET_USER")
    GROUP_ID=$(id -g "$TARGET_USER")

    echo "Bruker-ID: $USER_ID, Gruppe-ID: $GROUP_ID"
}


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
# 2. Installer dependencies (Inkludert Xorg/Kiosk)
###############################################
install_dependencies() {
    echo "--- Installerer systemavhengigheter ---"

    if command -v apt-get &> /dev/null; then
        echo "--- Ubuntu/Debian-basert system oppdaget ---"

        # 1. Installer Debian signeringsnøkkel
        apt-get update
        apt-get install -y debian-archive-keyring

        # 2. Legg til Debian bookworm repo for Chromium (signert)
        echo "deb [signed-by=/usr/share/keyrings/debian-archive-keyring.gpg] http://deb.debian.org/debian bookworm main" \
            | tee /etc/apt/sources.list.d/debian-chromium.list

        # 3. Oppdater pakker
        apt-get update

        # 4. Installer systemavhengigheter + Chromium DEB
        apt-get install -y \
            gphoto2 smartmontools hdparm openjdk-21-jdk rfkill jc network-manager \
            xorg xserver-xorg-input-libinput xinit \
            chromium chromium-common chromium-sandbox \
            unclutter matchbox-keyboard x11-xserver-utils \
            matchbox-window-manager

    elif command -v dnf &> /dev/null; then
        echo "--- Fedora/RHEL-basert system oppdaget ---"
        dnf check-update && dnf install -y \
            gphoto2 smartmontools hdparm java-21-openjdk-devel rfkill jc network-manager \
            xorg-x11-server-Xorg xorg-x11-drv-libinput xinit chromium \
            unclutter matchbox-keyboard x11-utils \
            matchbox-window-manager

    elif command -v pacman &> /dev/null; then
        echo "--- Arch-basert system oppdaget ---"
        pacman -Sy --noconfirm \
            gphoto2 smartmontools hdparm jdk21-openjdk rfkill jc network-manager \
            xorg-server xf86-input-libinput xorg-xinit chromium \
            unclutter matchbox-keyboard xorg-xinit \
            matchbox-window-manager

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
    echo "--- Konfigurerer eject-verktøy og sudoers ---"

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
# Kammich admin og nettverksstyring
Cmnd_Alias KAMMICH_ADMIN = /usr/sbin/smartctl, /usr/local/bin/kammich-eject
Cmnd_Alias KAMMICH_NETWORK = /usr/bin/systemctl restart hostapd, /usr/bin/systemctl stop hostapd, /usr/bin/systemctl start hostapd, /usr/bin/systemctl status hostapd, /usr/bin/systemctl restart dnsmasq, /usr/sbin/iw, /usr/bin/nmcli, /usr/bin/pkill, /usr/bin/kill, /usr/sbin/ip
%sudo ALL=(ALL) NOPASSWD: KAMMICH_ADMIN, KAMMICH_NETWORK
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
# 6. Kiosk Mode (Chromium + Xorg)
###############################################
configure_kiosk() {
    echo "--- Konfigurerer Kiosk-modus med touch-støtte ---"

    # Sikre at Xorg har SUID-rettigheter slik at kammich kan åpne VT1 uten sirkus
    if [ -f /usr/lib/xorg/Xorg ]; then
        chmod u+s /usr/lib/xorg/Xorg
    elif [ -f /usr/bin/Xorg ]; then
        chmod u+s /usr/bin/Xorg
    fi

    ###############################################
    # 1. Lag kiosk-skriptet
    ###############################################
    cat <<EOF > /usr/local/bin/kammich-kiosk
#!/bin/bash

# Vent til X-serveren svarer på display :0
until xdpyinfo -display :0 &>/dev/null; do
    sleep 0.5
done

# Deaktiver skjermsparing
xset s off
xset -dpms
xset s noblank

# Chromium krever crashpad-database og riktige rettigheter
mkdir -p /home/kammich/.config/chromium/Crashpad
chown -R kammich:kammich /home/kammich/.config/chromium

# Fjern låsefiler
rm -rf /home/kammich/.config/chromium/Default/Singleton*

# Skjul mus
unclutter -idle 1 &

# Skjermtastatur
matchbox-keyboard &

# Start Chromium
exec chromium \
    --kiosk http://localhost:8080 \
    --noerrdialogs \
    --disable-infobars \
    --touch-events=enabled \
    --overscroll-history-navigation=0 \
    --disable-pinch \
    --disable-session-crashed-bubble \
    --disable-features=TranslateUI \
    --enable-crashpad
EOF

    chmod +x /usr/local/bin/kammich-kiosk
    chown kammich:kammich /usr/local/bin/kammich-kiosk


    ###############################################
    # 2. Lag Xorg-tjenesten (starter GUI)
    ###############################################
    cat <<EOF > /etc/systemd/system/kammich-x.service
[Unit]
Description=Kammich Xorg Session
After=systemd-user-sessions.service network.target
Requires=systemd-user-sessions.service

[Service]
User=kammich
Group=kammich

# Kritisk: gi Xorg tilgang til VT1
TTYPath=/dev/tty1
StandardInput=tty
StandardOutput=journal
StandardError=journal

# Kritisk: Xorg må ha rettigheter til GPU/input
SupplementaryGroups=video input tty audio

Environment=DISPLAY=:0
Environment=XAUTHORITY=/home/kammich/.Xauthority

ExecStart=/usr/bin/Xorg :0 vt1 -keeptty -verbose 3 -logfile /var/log/Xorg-kammich.log

Restart=always
RestartSec=2

[Install]
WantedBy=multi-user.target
EOF


    ###############################################
    # 3. Lag kiosk-tjenesten (starter Chromium)
    ###############################################
    cat <<EOF > /etc/systemd/system/kammich-kiosk.service
[Unit]
Description=Kammich Chromium Kiosk
After=kammich-x.service
Requires=kammich-x.service

[Service]
User=kammich
Group=kammich
Environment=DISPLAY=:0
ExecStart=/usr/local/bin/kammich-kiosk
Restart=always
RestartSec=2

[Install]
WantedBy=multi-user.target
EOF


    # 4. Aktiver tjenester
    systemctl daemon-reload
    systemctl enable kammich-x.service
    systemctl enable kammich-kiosk.service

    echo "--- Kiosk-modus er konfigurert ---"
}

###############################################
# 7. Reload og trigger
###############################################
apply_changes() {
    echo "--- Reloading daemon and udev ---"
    systemctl daemon-reload
    udevadm control --reload-rules && udevadm trigger

    systemctl restart kammich-x.service
    sleep 2
    systemctl restart kammich-kiosk.service
    echo "--- Kammich-miljøet er nå klargjort og kjører! ---"
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
configure_kiosk
apply_changes