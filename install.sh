#!/bin/bash

# Sjekk at skriptet kjøres som root
if [[ $EUID -ne 0 ]]; then
    echo "[-] Dette skriptet må kjøres som root (sudo)."
    exit 1
fi

TARGET_USER="kammich"
MOUNT_ROOT="/run/kammich/removable"
STATE_ROOT="/run/kammich"

echo "[+] Starter komplett installasjon for Kammich-miljøet..."

###############################################
# 1. Fjern Snap-dritt og "falske" apt-pakker
###############################################
echo "[*] Fjerner eventuelle Snap-rester og stubber..."
systemctl stop snapd.service snapd.socket snapd.seeded.service 2>/dev/null
systemctl disable snapd.service snapd.socket snapd.seeded.service 2>/dev/null

if command -v snap &> /dev/null; then
    for snap_pkg in $(snap list | awk 'NR>1 {print $1}'); do
        snap remove --purge "$snap_pkg" 2>/dev/null
    done
fi

dpkg --remove --force-remove-reinstreq chromium chromium-browser chromium-browser-l10n chromium-codecs-ffmpeg-extra firefox firefox-locale-en thunderbird gnome-software 2>/dev/null
apt-get purge -y snapd gnome-software-plugin-snap 2>/dev/null
apt-get autoremove --purge -y 2>/dev/null

rm -rf /snap /var/snap /var/lib/snapd /var/cache/snapd /etc/snapd ~/snap

cat << 'EOF' > /etc/apt/preferences.d/no-snap.pref
Package: snapd
Pin: release *
Pin-Priority: -1
EOF

###############################################
# 2. Opprett dedikert bruker
###############################################
echo "[*] Oppretter/sjekker brukeren $TARGET_USER..."
if ! id "$TARGET_USER" &>/dev/null; then
    adduser --disabled-password --gecos "" "$TARGET_USER"
fi

mkdir -p /home/$TARGET_USER/logs
chown -R "$TARGET_USER:$TARGET_USER" /home/$TARGET_USER
chmod 755 /home/$TARGET_USER

# Gi brukeren tilgang til nødvendige grupper (inkludert seat for Wayland)
usermod -aG input,tty,audio,video,seat "$TARGET_USER"

USER_ID=$(id -u "$TARGET_USER")
GROUP_ID=$(id -g "$TARGET_USER")

###############################################
# 3. Installer systemavhengigheter + Wayland-kiosk
###############################################
echo "[*] Installerer pakker (Java, Chromium, Wayland-komponenter, seatd)..."
apt-get update
apt-get install -y \
    gphoto2 smartmontools hdparm openjdk-21-jdk rfkill jc network-manager \
    seatd cage wvkbd

apt-get install -y \
    chromium chromium-common

# Aktiver seatd-tjenesten systemomspennende
systemctl enable --now seatd

###############################################
# 4. Setup Base-struktur (USB Mount)
###############################################
echo "[*] Setter opp mapper og udev-regler for USB..."
mkdir -p "$MOUNT_ROOT"
echo "MOUNT_ROOT=\"$MOUNT_ROOT\"" > /etc/kammich.conf
echo "STATE_ROOT=\"$STATE_ROOT\"" >> /etc/kammich.conf

cat <<EOF > /etc/tmpfiles.d/kammich.conf
d $STATE_ROOT 0755 root root -
EOF

cat <<EOF > /etc/udev/rules.d/99-kammich.rules
ACTION=="add", SUBSYSTEM=="block", ENV{ID_BUS}=="usb", KERNEL=="sd[a-z][0-9]|nvme[0-9]n[0-9]p[0-9]", ENV{SYSTEMD_WANTS}+="usb-mount@%k.service"
ACTION=="add", SUBSYSTEM=="block", SUBSYSTEMS=="usb", KERNEL=="sd[a-z][0-9]", ENV{SYSTEMD_WANTS}+="usb-mount@%k.service"
ACTION=="remove", SUBSYSTEM=="block", KERNEL=="sd[a-z][0-9]|nvme[0-9]n[0-9]p[0-9]", ENV{SYSTEMD_WANTS}+="usb-unmount@%k.service"
EOF

###############################################
# 5. Mount/Unmount Helper & Eject
###############################################
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

cat <<'EOF' > /usr/local/bin/kammich-eject
#!/bin/bash
source /etc/kammich.conf
DEV="$1"
MODEL_FILE="${STATE_ROOT}/${DEV}.model"
[ ! -f "$MODEL_FILE" ] && exit 1
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

###############################################
# 6. Sudoers & Network Forwarding
###############################################
echo "[*] Konfigurerer sudoers og nettverk..."
cat << 'EOF' > /etc/sudoers.d/kammich
Cmnd_Alias KAMMICH_ADMIN = /usr/sbin/smartctl, /usr/local/bin/kammich-eject
Cmnd_Alias KAMMICH_NETWORK = /usr/bin/systemctl restart hostapd, /usr/bin/systemctl stop hostapd, /usr/bin/systemctl start hostapd, /usr/bin/systemctl status hostapd, /usr/bin/systemctl restart dnsmasq, /usr/sbin/iw, /usr/bin/nmcli, /usr/bin/pkill, /usr/bin/kill, /usr/sbin/ip
kammich ALL=(ALL) NOPASSWD: KAMMICH_ADMIN, KAMMICH_NETWORK
EOF
chmod 0440 /etc/sudoers.d/kammich

echo "net.ipv4.ip_forward=1" > /etc/sysctl.d/99-kammich-forwarding.conf
sysctl -p /etc/sysctl.d/99-kammich-forwarding.conf

###############################################
# 7. Kiosk Oppsett (Cage + wvkbd + Chromium)
###############################################
echo "[*] Konfigurerer Wayland-kiosk skript og tjeneste..."

cat << 'EOF' > /usr/local/bin/kammich-kiosk
#!/bin/bash
LOG_FILE="/home/kammich/logs/kiosk.log"
exec > >(tee -a "$LOG_FILE") 2>&1

echo "=== Kiosk startet: $(date) ==="

# Start virtuelt tastatur i bakgrunnen (skjult til felt får fokus)
wvkbd-mobintl --hidden --landscape-layers "simple,special" --alpha 200 &

# Start Cage med Chromium
exec cage -- chromium \
    --kiosk \
    --no-first-run \
    --disable-infobars \
    --disable-session-crashed-bubble \
    --disable-features=TranslateUI \
    --enable-features=TouchpadOverscrollHistoryNavigation=0 \
    http://localhost:8080
EOF

chmod +x /usr/local/bin/kammich-kiosk
chown kammich:kammich /usr/local/bin/kammich-kiosk

cat << EOF > /etc/systemd/system/kammich-kiosk.service
[Unit]
Description=Kammich Wayland Kiosk (Cage + wvkbd + Chromium)
After=network.target local-fs.target seatd.service

[Service]
User=$TARGET_USER
Group=$TARGET_USER

RuntimeDirectory=kammich-kiosk
Environment=XDG_RUNTIME_DIR=/run/kammich-kiosk
Environment=WLR_BACKENDS=drm,libinput

ExecStart=/usr/local/bin/kammich-kiosk
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
EOF

###############################################
# 8. Aktiver alt
###############################################
echo "[*] Aktiverer systemd og udev..."
systemctl daemon-reload
udevadm control --reload-rules && udevadm trigger
systemctl enable kammich-kiosk.service

echo "[+] Installasjonen er fullført! Start kiosken med: sudo systemctl start kammich-kiosk.service"
