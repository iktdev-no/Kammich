#!/bin/bash

# Sjekk root
if [[ $EUID -ne 0 ]]; then
    echo "Dette scriptet må kjøres som root (sudo)"
    exit 1
fi

TARGET_USER="kammich"
MOUNT_ROOT="/run/kammich/removable"
STATE_ROOT="/run/kammich"
APP_DATA_ROOT="/var/lib/kammich"
VENV_DIR="$APP_DATA_ROOT/kiosk-env"

echo "[+] Setter opp Kammich med egen backend-tjeneste og pykiosk..."

###############################################
# 1. Sett opp Debian-repositorium (om nødvendig)
###############################################
echo "[*] Konfigurerer system-repoer..."
mkdir -p /etc/apt/keyrings
curl -fsSL https://ftp-master.debian.org/keys/archive-key-12.asc | gpg --yes --dearmor -o /etc/apt/keyrings/debian-archive-keyring.gpg

###############################################
# 2. Opprett dedikert bruker og grupper
###############################################
echo "[*] Oppretter bruker og sikrer systemgrupper..."
for grp in input tty audio video plugdev; do
    getent group "$grp" &>/dev/null || groupadd "$grp"
done

if ! id "$TARGET_USER" &>/dev/null; then
    adduser --disabled-password --gecos "" "$TARGET_USER"
fi

usermod -aG input,tty,audio,video,render "$TARGET_USER"

USER_ID=$(id -u "$TARGET_USER")
GROUP_ID=$(id -g "$TARGET_USER")

systemctl daemon-reload

###############################################
# 3. Installer avhengigheter
###############################################
echo "[*] Installerer systemavhengigheter..."
apt-get update
apt-get install -y \
    gphoto2 smartmontools hdparm openjdk-21-jdk rfkill jc network-manager iw \
    xserver-xorg x11-xserver-utils xinit openbox onboard python3-tk xdotool dbus-x11 unclutter xinput \
    python3-dev python3-venv python3-pip python3-gi python3-gi-cairo \
    python3-cairo gir1.2-gtk-3.0 gir1.2-webkit2-4.1 jq

###############################################
# 4. Setup Base-struktur (USB Mount & /var/lib/kammich)
###############################################
echo "[*] Oppretter persistert lagringsrot..."
mkdir -p "$MOUNT_ROOT"
mkdir -p "$APP_DATA_ROOT/logs"

DEV_USER=${SUDO_USER:-$TARGET_USER}

chown -R "$TARGET_USER:$TARGET_USER" "$APP_DATA_ROOT"
chmod -R 775 "$APP_DATA_ROOT"
find "$APP_DATA_ROOT" -type d -exec chmod g+s {} +

echo "MOUNT_ROOT=\"$MOUNT_ROOT\"" > /etc/kammich.conf
echo "STATE_ROOT=\"$STATE_ROOT\"" >> /etc/kammich.conf
echo "APP_DATA_ROOT=\"$APP_DATA_ROOT\"" >> /etc/kammich.conf

cat <<EOF > /etc/tmpfiles.d/kammich.conf
d $STATE_ROOT 0755 root root -
d $APP_DATA_ROOT 0775 $TARGET_USER $TARGET_USER -
EOF

cat <<EOF > /etc/udev/rules.d/99-kammich.rules
ACTION=="add", SUBSYSTEM=="block", ENV{ID_BUS}=="usb", KERNEL=="sd[a-z][0-9]|nvme[0-9]n[0-9]p[0-9]", ENV{SYSTEMD_WANTS}+="usb-mount@%k.service"
ACTION=="add", SUBSYSTEM=="block", SUBSYSTEMS=="usb", KERNEL=="sd[a-z][0-9]", ENV{SYSTEMD_WANTS}+="usb-mount@%k.service"
ACTION=="remove", SUBSYSTEM=="block", KERNEL=="sd[a-z][0-9]|nvme[0-9]n[0-9]p[0-9]", ENV{SYSTEMD_WANTS}+="usb-unmount@%k.service"
EOF

###############################################
# 5. Sett opp Python Virtual Environment for pykiosk under /var/lib/kammich
###############################################
echo "[*] Setter opp Python Virtual Environment (venv)..."
rm -rf "$VENV_DIR"

sudo -u "$TARGET_USER" python3 -m venv --system-site-packages "$VENV_DIR"
sudo -u "$TARGET_USER" "$VENV_DIR/bin/pip" install --upgrade pip
sudo -u "$TARGET_USER" "$VENV_DIR/bin/pip" install pykiosk -U

###############################################
# 5.1 Oppdateringsskript for PyKiosk
###############################################

cat <<'EOF' > /usr/local/bin/update-pykiosk
#!/bin/bash

if [[ $EUID -ne 0 ]]; then
    echo "Dette scriptet må kjøres som root (sudo)"
    exit 1
fi

TARGET_USER="kammich"
VENV_DIR="/var/lib/kammich/kiosk-env"

echo "[+] Stopper Kammich kiosk..."
systemctl stop kammich-kiosk.service

echo "[+] Oppdaterer pip..."
sudo -u "$TARGET_USER" "$VENV_DIR/bin/pip" install --upgrade pip

echo "[+] Oppdaterer PyKiosk..."
sudo -u "$TARGET_USER" "$VENV_DIR/bin/pip" install --upgrade pykiosk

echo "[+] Starter Kammich kiosk..."
systemctl start kammich-kiosk.service

echo "[+] PyKiosk er oppdatert!"
EOF

chmod +x /usr/local/bin/update-pykiosk


###############################################
# 6. Last ned siste versjon av Kammich (Kammich.jar)
###############################################
echo "[*] Laster ned siste versjon av Kammich fra GitHub Releases..."
RELEASES_JSON=$(curl -fsSL \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2026-03-10" \
  "https://api.github.com/repos/iktdev-no/Kammich/releases") || {
    echo "[!] Kritisk: Klarte ikke hente releases fra GitHub API!"
    exit 1
}

LATEST_JAR_URL=$(printf '%s' "$RELEASES_JSON" \
  | grep -o '"browser_download_url": "[^"]*\.jar"' \
  | head -n 1 \
  | cut -d '"' -f 4)

if [ -z "$LATEST_JAR_URL" ]; then
    echo "[!] Kritisk: GitHub API svarte, men ingen JAR-fil ble funnet!"
    exit 1
fi

echo "[+] Fant JAR: $LATEST_JAR_URL"

sudo -u "$TARGET_USER" curl -sL "$LATEST_JAR_URL" -o "$APP_DATA_ROOT/Kammich.jar"
echo "[+] Kammich.jar er lastet ned til $APP_DATA_ROOT/"

###############################################
# 7. Mount/Unmount Helper & Eject
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
    [ -z "\$FSTYPE" ] && exit 0

    mkdir -p "\$TARGET"
    grep -qs "\$TARGET" /proc/mounts && exit 0

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
# 8. Sudoers & Network Forwarding
###############################################
cat << EOF > /etc/sudoers.d/kammich
Cmnd_Alias KAMMICH_ADMIN = /usr/sbin/smartctl, /usr/local/bin/kammich-eject, /usr/bin/xrandr, /usr/bin/xinput, /usr/bin/systemctl poweroff, /usr/bin/systemctl reboot, /usr/bin/systemctl restart kammich-backend.service
Cmnd_Alias KAMMICH_NETWORK = /usr/bin/systemctl restart hostapd, /usr/bin/systemctl stop hostapd, /usr/bin/systemctl start hostapd, /usr/bin/systemctl status hostapd, /usr/bin/systemctl restart dnsmasq, /usr/sbin/iw, /usr/bin/nmcli, /usr/bin/pkill, /usr/bin/kill, /usr/sbin/ip

$TARGET_USER ALL=(ALL) NOPASSWD: KAMMICH_ADMIN, KAMMICH_NETWORK
$DEV_USER ALL=(ALL) NOPASSWD: KAMMICH_ADMIN, KAMMICH_NETWORK
EOF

chmod 0440 /etc/sudoers.d/kammich
visudo -cf /etc/sudoers.d/kammich
echo "net.ipv4.ip_forward=1" > /etc/sysctl.d/99-kammich-forwarding.conf
sysctl -p /etc/sysctl.d/99-kammich-forwarding.conf

###############################################
# 9. Kiosk Startskript (plassert i /var/lib/kammich)
###############################################
cat << 'EOF' > /var/lib/kammich/kiosk-start.sh
#!/bin/bash
export DISPLAY=:0
export XAUTHORITY=$HOME/.Xauthority

if [ -z "$DBUS_SESSION_BUS_ADDRESS" ]; then
    eval $(dbus-launch --sh-syntax)
    export DBUS_SESSION_BUS_ADDRESS
    export DBUS_SESSION_BUS_PID
fi

SCREENSAVER_TIMEOUT=300

xset +dpms
xset s off
xset dpms $SCREENSAVER_TIMEOUT $SCREENSAVER_TIMEOUT $SCREENSAVER_TIMEOUT

gsettings set org.onboard theme 'Nightshade' 2>/dev/null

# Vent på at Spring Boot-backenden svarer på port 8080 før vi starter nettleseren/kiosken
echo "Venter på at Kammich backend skal starte..."
until curl -s http://localhost:8080 > /dev/null; do
    sleep 1
done
echo "Backend er oppe! Starter kiosk..."

# Start Openbox
openbox-session &

sleep 1

# Start pykiosk fra /var/lib/kammich/kiosk-env
exec /var/lib/kammich/kiosk-env/bin/pykiosk
EOF

chown kammich:kammich /var/lib/kammich/kiosk-start.sh
chmod +x /var/lib/kammich/kiosk-start.sh

###############################################
# 10. Systemd Tjenester (Backend + Kiosk)
###############################################
echo "[*] Oppretter systemd-tjenester..."

# 10.1 Java Backend Tjeneste (Peker nå på Kammich.jar)
cat << 'EOF' > /etc/systemd/system/kammich-backend.service
[Unit]
Description=Kammich Java Backend Service
After=network.target sound.target

[Service]
User=kammich
Group=kammich
WorkingDirectory=/var/lib/kammich
ExecStart=/usr/bin/java -jar /var/lib/kammich/Kammich.jar
Restart=always
RestartSec=5
StandardOutput=append:/var/lib/kammich/logs/backend.log
StandardError=append:/var/lib/kammich/logs/backend_error.log

[Install]
WantedBy=multi-user.target
EOF

# 10.2 Kiosk GUI Tjeneste
cat << 'EOF' > /etc/systemd/system/kammich-kiosk.service
[Unit]
Description=Kammich Python Kiosk Service (pykiosk + openbox)
After=network.target kammich-backend.service udev.service

[Service]
User=kammich
Group=kammich
Environment=DISPLAY=:0
ExecStart=/usr/bin/startx /var/lib/kammich/kiosk-start.sh -- :0 vt1 -nocursor
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

###############################################
# 11. Aktiver og start tjenester
###############################################
echo "[*] Aktiverer og starter tjenester..."
systemctl daemon-reload
systemctl disable getty@tty1.service 2>/dev/null

systemctl enable kammich-backend.service
systemctl restart kammich-backend.service

systemctl enable kammich-kiosk.service
systemctl restart kammich-kiosk.service

udevadm control --reload-rules && udevadm trigger

echo "[+] Alt er klart! Kammich.jar hentes dynamisk og alt kjører rent under /var/lib/kammich."
