#!/bin/bash

# Sjekk om scriptet kjøres som root
if [[ $EUID -ne 0 ]]; then
   echo "Dette scriptet må kjøres som root (sudo)"
   exit 1
fi

echo "--- Starter forberedelse av Kammich-miljø ---"

# 1. Identifiser pakkebehandler
if command -v apt-get &> /dev/null; then
    PKG_MGR="apt-get install -y"
    UPDATE_CMD="apt-get update"
elif command -v dnf &> /dev/null; then
    PKG_MGR="dnf install -y"
    UPDATE_CMD="dnf check-update"
elif command -v pacman &> /dev/null; then
    PKG_MGR="pacman -S --noconfirm"
    UPDATE_CMD="pacman -Sy"
else
    echo "Kunne ikke identifisere pakkebehandler. Installer gphoto2 og smartmontools manuelt."
    exit 1
fi

# 2. Installer dependencies
echo "Installerer systemavhengigheter..."
$UPDATE_CMD
$PKG_MGR gphoto2 smartmontools hdparm

# 3. Udev-regler for systemd-automount
echo "Konfigurerer udev-regler..."
cat <<EOF > /etc/udev/rules.d/99-kammich.rules
ACTION=="add", SUBSYSTEM=="block", KERNEL=="sd[a-z][0-9]", ENV{SYSTEMD_WANTS}+="usb-mount@%k.service"
ACTION=="remove", SUBSYSTEM=="block", KERNEL=="sd[a-z][0-9]", ENV{SYSTEMD_WANTS}+="usb-unmount@%k.service"
EOF

# 4. Systemd mount-service
echo "Oppretter systemd mount-service..."
cat <<EOF > /etc/systemd/system/usb-mount@.service
[Unit]
Description=Automount USB device %I
After=systemd-udevd.service

[Service]
Type=oneshot
Environment=USER_ID=$(id -u $USER)
Environment=GROUP_ID=$(id -g $USER)
ExecStart=/usr/bin/mkdir -p /media/removable/%I
ExecStart=/usr/bin/mount -o uid=${USER_ID},gid=${GROUP_ID},umask=000,fmask=000,dmask=000 /dev/%I /media/removable/%I

[Install]
WantedBy=multi-user.target
EOF


# 5. Systemd unmount-service
echo "Oppretter systemd unmount-service..."
cat <<EOF > /etc/systemd/system/usb-unmount@.service
[Unit]
Description=Auto-unmount USB device %I

[Service]
Type=oneshot
ExecStart=/usr/bin/umount /media/removable/%I
ExecStart=/usr/bin/rmdir /media/removable/%I
EOF

# 6. Eject-service (safe removal)
echo "Oppretter systemd eject-service..."
cat <<EOF > /etc/systemd/system/usb-eject@.service
[Unit]
Description=Safe eject USB device %I
After=usb-unmount@%I.service

[Service]
Type=oneshot
ExecStart=/usr/local/bin/kammich-eject %I
EOF

# 7. Eject wrapper-script
echo "Oppretter eject wrapper-script..."
cat <<'EOF' > /usr/local/bin/kammich-eject
#!/bin/bash
DEV="$1"

# Sync all buffers
sync

# Unmount if still mounted
if mountpoint -q /media/removable/"$DEV"; then
    umount /media/removable/"$DEV"
fi

# Spindown (hvis disk støtter det)
hdparm -Y /dev/"$DEV" 2>/dev/null

# Forsøk USB power-off (hvis tilgjengelig)
USB_PATH=$(readlink -f /sys/block/"$DEV"/device)
if [[ -e "$USB_PATH/usb_device/authorized" ]]; then
    echo 0 > "$USB_PATH/usb_device/authorized"
fi

exit 0
EOF

chmod +x /usr/local/bin/kammich-eject

# 8. sudoers-konfigurasjon for smartctl og eject
USER_NAME=${SUDO_USER:-$USER}
echo "Konfigurerer sudoers..."
cat <<EOF > /etc/sudoers.d/kammich
$USER_NAME ALL=(ALL) NOPASSWD: /usr/sbin/smartctl
$USER_NAME ALL=(ALL) NOPASSWD: /usr/local/bin/kammich-eject
EOF

# 9. Reload
systemctl daemon-reload
udevadm control --reload-rules && udevadm trigger

echo "--- Installasjon fullført ---"
echo "Kammich-miljøet er nå klargjort."
