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
        apt-get update && apt-get install -y gphoto2 smartmontools hdparm
    elif command -v dnf &> /dev/null; then
        dnf check-update && dnf install -y gphoto2 smartmontools hdparm
    elif command -v pacman &> /dev/null; then
        pacman -Sy --noconfirm gphoto2 smartmontools hdparm
    else
        echo "Kunne ikke identifisere pakkebehandler."
        exit 1
    fi
}

###############################################
# 3. Udev-regler (kun trigger, ingen RUN)
###############################################
configure_udev() {
    echo "--- Konfigurerer udev-regler ---"

    cat <<EOF > /etc/udev/rules.d/99-kammich.rules
ACTION=="add", SUBSYSTEM=="block", ENV{ID_BUS}=="usb", KERNEL=="sd[a-z][0-9]|nvme[0-9]n[0-9]p[0-9]", ENV{SYSTEMD_WANTS}+="usb-mount@%k.service"
ACTION=="remove", SUBSYSTEM=="block", ENV{ID_BUS}=="usb", KERNEL=="sd[a-z][0-9]|nvme[0-9]n[0-9]p[0-9]", ENV{SYSTEMD_WANTS}+="usb-unmount@%k.service"
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
ACTION=\$1; DEV=\$2
MODEL_FILE="$STATE_ROOT/\${DEV}.model"

# DEBUG: Sjekk om variablene er satt
if [ -z "\$MOUNT_ROOT" ] || [ -z "\$STATE_ROOT" ]; then
    echo "ERROR: Konfigurasjon ikke lastet! Sjekk /etc/kammich.conf" >&2
    exit 1
fi

MODEL=\$(cat "\$MODEL_FILE" | tr -d '[:space:]')
MODEL=\${MODEL:-Unknown_Device}

TARGET="\$MOUNT_ROOT/\$MODEL/\$DEV"
mkdir -p "\$TARGET"

# DEBUG: Vis hva vi prøver
echo "DEBUG: Mounter /dev/\$DEV til \$TARGET" >&2

if ! mount -o uid=$USER_ID,gid=$GROUP_ID,umask=000,fmask=000,dmask=000 "/dev/\$DEV" "\$TARGET" 2>/dev/null; then
    mount "/dev/\$DEV" "\$TARGET"
fi
EOF
    chmod +x /usr/local/bin/kammich-mount-helper

    # 2. Oppdatert Mount Service
    cat <<EOF > /etc/systemd/system/usb-mount@.service
[Unit]
Description=Automount USB device %I
BindsTo=dev-%i.device
After=dev-%i.device systemd-udevd.service

[Service]
Type=oneshot
ExecStartPre=/usr/local/bin/kammich-mount-helper prepare %I
ExecStart=/usr/local/bin/kammich-mount-helper mount %I
EOF

    # 3. Unmount Service
    cat <<EOF > /etc/systemd/system/usb-unmount@.service
[Unit]
Description=Auto-unmount USB device %I
[Service]
Type=oneshot
ExecStart=/bin/bash -c 'MODEL=\$(cat $STATE_ROOT/%I.model 2>/dev/null || echo "Unknown_Device"); umount "$MOUNT_ROOT/\$MODEL/%I" 2>/dev/null; rm -f "$STATE_ROOT/%I.model"'
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
    cat <<EOF > /etc/sudoers.d/kammich
$TARGET_USER ALL=(ALL) NOPASSWD: /usr/sbin/smartctl, /usr/local/bin/kammich-eject
EOF
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
