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

echo "[+] Starter komplett og robust oppsett av Kammich Kiosk..."

###############################################
# 0. Full opprydding av gamle feilede løsninger
###############################################
cleanup_system() {
    echo "[*] Kjører systemrydding (cleanup)..."
    systemctl stop lightdm kammich-kiosk.service 2>/dev/null
    systemctl disable lightdm kammich-kiosk.service 2>/dev/null
    rm -f /etc/systemd/system/kammich-kiosk.service

    if command -v snap &> /dev/null; then
        for snap_pkg in $(snap list | awk 'NR>1 {print $1}'); do
            snap remove --purge "$snap_pkg" 2>/dev/null
        done
    fi

    apt-get purge -y lightdm openbox lxde-core lxde lxde-common touchegg 2>/dev/null
    dpkg --remove --force-remove-reinstreq chromium chromium-browser chromium-browser-l10n chromium-codecs-ffmpeg-extra firefox firefox-locale-en thunderbird gnome-software 2>/dev/null
    apt-get purge -y snapd gnome-software-plugin-snap 2>/dev/null
    apt-get autoremove --purge -y 2>/dev/null

    rm -rf /snap /var/snap /var/lib/snapd /var/cache/snapd /etc/snapd ~/snap
    rm -rf /etc/systemd/system/getty@tty1.service.d
}

cleanup_system

cat << 'EOF' > /etc/apt/preferences.d/no-snap.pref
Package: snapd
Pin: release *
Pin-Priority: -1
EOF

###############################################
# 1. Sett opp Debian-repositorium for ren Chromium
###############################################
echo "[*] Konfigurerer Debian-repo for ren Chromium..."
mkdir -p /etc/apt/keyrings
curl -fsSL https://ftp-master.debian.org/keys/archive-key-12.asc | gpg --yes --dearmor -o /etc/apt/keyrings/debian-archive-keyring.gpg

cat << 'EOF' > /etc/apt/preferences.d/debian-chromium
Package: chromium chromium-common chromium-sandbox
Pin: release o=Debian
Pin-Priority: 900
EOF

cat << 'EOF' > /etc/apt/sources.list.d/debian-chromium.list
deb [signed-by=/etc/apt/keyrings/debian-archive-keyring.gpg] http://deb.debian.org/debian bookworm main
EOF

###############################################
# 2. Opprett dedikert bruker og grupper
###############################################
echo "[*] Oppretter bruker og sikrer systemgrupper..."
for grp in input tty audio video; do
    getent group "$grp" &>/dev/null || groupadd "$grp"
done

if ! id "$TARGET_USER" &>/dev/null; then
    adduser --disabled-password --gecos "" "$TARGET_USER"
fi

mkdir -p /home/$TARGET_USER/logs
chown -R "$TARGET_USER:$TARGET_USER" /home/$TARGET_USER
chmod 755 /home/$TARGET_USER
usermod -aG input,tty,audio,video,render "$TARGET_USER"

USER_ID=$(id -u "$TARGET_USER")
GROUP_ID=$(id -g "$TARGET_USER")

systemctl daemon-reload

###############################################
# 3. Installer avhengigheter (Inkl. xinput)
###############################################
echo "[*] Installerer systemavhengigheter..."
apt-get update
apt-get install -y \
    gphoto2 smartmontools hdparm openjdk-21-jdk rfkill jc network-manager iw \
    xserver-xorg x11-xserver-utils xinit onboard python3-tk xdotool chromium chromium-common dbus-x11 unclutter xinput

chmod u+s /usr/lib/xorg/Xorg 2>/dev/null || chmod u+s /usr/bin/Xorg 2>/dev/null

###############################################
# 4. Deaktiver Hibernate / Sleep
###############################################
echo "[*] Deaktiverer dvale/hibernate og nettverkstvang under boot..."

cat << 'EOF' > /etc/systemd/sleep.conf
[Sleep]
AllowSuspend=no
AllowHibernation=no
AllowSuspendThenHibernate=no
AllowHybridSleep=no
EOF

systemctl mask sleep.target suspend.target hibernation.target hybrid-sleep.target
systemctl mask NetworkManager-wait-online.service

###############################################
# 5. Setup Base-struktur (USB Mount & /var/lib/kammich)
###############################################
echo "[*] Oppretter persistert lagringsrot og setter fleksible rettigheter for $TARGET_USER og utvikler..."
mkdir -p "$MOUNT_ROOT"
mkdir -p "$APP_DATA_ROOT"

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
echo "[*] Setter opp udev-regler for usb"

cat <<EOF > /etc/udev/rules.d/99-kammich.rules
ACTION=="add", SUBSYSTEM=="block", ENV{ID_BUS}=="usb", KERNEL=="sd[a-z][0-9]|nvme[0-9]n[0-9]p[0-9]", ENV{SYSTEMD_WANTS}+="usb-mount@%k.service"
ACTION=="add", SUBSYSTEM=="block", SUBSYSTEMS=="usb", KERNEL=="sd[a-z][0-9]", ENV{SYSTEMD_WANTS}+="usb-mount@%k.service"
ACTION=="remove", SUBSYSTEM=="block", KERNEL=="sd[a-z][0-9]|nvme[0-9]n[0-9]p[0-9]", ENV{SYSTEMD_WANTS}+="usb-unmount@%k.service"
EOF

###############################################
# 6. Mount/Unmount Helper & Eject
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
# 7. Sudoers & Network Forwarding
###############################################
echo "[*] Konfigurerer sudoers og nettverk for både $TARGET_USER og $DEV_USER..."

cat << EOF > /etc/sudoers.d/kammich
Cmnd_Alias KAMMICH_ADMIN = /usr/sbin/smartctl, /usr/local/bin/kammich-eject, /usr/bin/xrandr, /usr/bin/xinput
Cmnd_Alias KAMMICH_NETWORK = /usr/bin/systemctl restart hostapd, /usr/bin/systemctl stop hostapd, /usr/bin/systemctl start hostapd, /usr/bin/systemctl status hostapd, /usr/bin/systemctl restart dnsmasq, /usr/sbin/iw, /usr/bin/nmcli, /usr/bin/pkill, /usr/bin/kill, /usr/sbin/ip

$TARGET_USER ALL=(ALL) NOPASSWD: KAMMICH_ADMIN, KAMMICH_NETWORK
$DEV_USER ALL=(ALL) NOPASSWD: KAMMICH_ADMIN, KAMMICH_NETWORK
EOF

chmod 0440 /etc/sudoers.d/kammich

if visudo -cf /etc/sudoers.d/kammich; then
    echo "Suksess: sudoers-filen er validert."
else
    echo "FEIL: Sudoers-validering feilet!"
    exit 1
fi

echo "net.ipv4.ip_forward=1" > /etc/sysctl.d/99-kammich-forwarding.conf
sysctl -p /etc/sysctl.d/99-kammich-forwarding.conf

###############################################
# 8. Kiosk Manager (Python / Tkinter med husket rotasjon og persistent tilstand)
###############################################
cat << 'EOF' > /home/kammich/kiosk-manager.py
import tkinter as tk
import subprocess
import os
import time

startUrl = "http://192.168.2.20:5173"
ROTATION_FILE = "/var/lib/kammich/rotation.idx"

# Rekkefølge: 0 (normal), 90 (right), 180 (inverted), 270 (left)
rotations = [
    ("normal", "1 0 0 0 1 0 0 0 1"),
    ("right", "0 1 0 -1 0 1 0 0 1"),
    ("inverted", "-1 0 1 0 -1 1 0 0 1"),
    ("left", "0 -1 1 1 0 0 0 0 1")
]

# Les inn lagret rotasjonsindeks hvis den finnes
current_rot_idx = 0
if os.path.exists(ROTATION_FILE):
    try:
        with open(ROTATION_FILE, "r") as f:
            current_rot_idx = int(f.read().strip())
            if current_rot_idx < 0 or current_rot_idx >= len(rotations):
                current_rot_idx = 0
    except:
        current_rot_idx = 0

def go_back():
    subprocess.run(["xdotool", "key", "Alt+Left"])

def go_home():
    subprocess.run(["xdotool", "key", "ctrl+l"])
    time.sleep(0.1)
    subprocess.run(["xdotool", "type", startUrl])
    subprocess.run(["xdotool", "key", "Return"])

def toggle_keyboard():
    res = subprocess.run(["pgrep", "onboard"], capture_output=True)
    if res.returncode != 0:
        subprocess.Popen(["onboard"])
    else:
        os.system("pkill onboard")

def apply_rotation(idx, initial=False):
    global chromium_proc
    rot_name, matrix = rotations[idx]

    res = subprocess.run("xrandr | grep ' connected' | awk '{print $1}'", shell=True, capture_output=True, text=True)
    output = res.stdout.strip()

    if output:
        old_w = root.winfo_screenwidth()
        old_h = root.winfo_screenheight()

        # 1. Roter skjermen
        subprocess.run(["sudo", "xrandr", "--output", output, "--rotate", rot_name])

        # 2. Roter touch-matrisen
        xinput_res = subprocess.run(["xinput", "list", "--name-only"], capture_output=True, text=True)
        for dev in xinput_res.stdout.splitlines():
            if any(k in dev.lower() for k in ["touch", "digitizer", "pen", "stylus", "ctp"]):
                subprocess.run(["sudo", "xinput", "set-prop", dev, "Coordinate Transformation Matrix"] + matrix.split())

        # 3. Vent på X11
        time.sleep(0.5)

        # 4. Beregn nye mål
        new_width = root.winfo_screenwidth()
        new_height = root.winfo_screenheight()

        if rot_name in ["left", "right"] and new_width == old_w and new_height == old_h:
            new_width, new_height = old_h, old_w

        new_web_height = new_height - bar_height

        # 5. Resize Tkinter-baren
        root.geometry(f"{new_width}x{bar_height}+0+{new_web_height}")

        # 6. Start eller restart Chromium med riktige mål
        if not initial and chromium_proc:
            chromium_proc.terminate()
            try:
                chromium_proc.wait(timeout=2)
            except subprocess.TimeoutExpired:
                chromium_proc.kill()

        os.system("pkill -f chromium")

        chromium_proc = subprocess.Popen([
            "chromium",
            "--kiosk",
            f"--window-size={new_width},{new_web_height}",
            "--window-position=0,0",
            "--no-first-run",
            "--no-default-browser-check",
            "--disable-infobars",
            "--disable-session-crashed-bubble",
            "--disable-dev-shm-usage",
            "--password-store=basic",
            "--touch-events=enabled",
            "--no-cursor",
            "--overscroll-history-navigation=0",
            "--user-data-dir=/home/kammich/.config/chromium",
            startUrl
        ])

        # 7. Tilpass tastaturet riktig
        kb_w = int(new_width * 0.90)
        kb_h = min(260, int(new_height * 0.40))
        kb_x = int((new_width - kb_w) / 2)
        kb_y = int(new_height - kb_h - bar_height - 10)

        orientation = "portrait" if new_height > new_width else "landscape"

        subprocess.run(["gsettings", "set", f"org.onboard.window.{orientation}", "width", str(kb_w)])
        subprocess.run(["gsettings", "set", f"org.onboard.window.{orientation}", "height", str(kb_h)])
        subprocess.run(["gsettings", "set", f"org.onboard.window.{orientation}", "x", str(kb_x)])
        subprocess.run(["gsettings", "set", f"org.onboard.window.{orientation}", "y", str(kb_y)])

def rotate_screen():
    global current_rot_idx
    current_rot_idx = (current_rot_idx + 1) % len(rotations)

    # Lagre ny rotasjonsindeks til disk
    try:
        with open(ROTATION_FILE, "w") as f:
            f.write(str(current_rot_idx))
    except Exception as e:
        print(f"Klarte ikke å lagre rotasjon: {e}")

    apply_rotation(current_rot_idx, initial=False)

subprocess.Popen(["onboard"])

root = tk.Tk()
root.overrideredirect(True)
root.attributes("-topmost", True)
root.focus_force()

screen_width = root.winfo_screenwidth()
screen_height = root.winfo_screenheight()

bar_height = 50
web_height = screen_height - bar_height

root.geometry(f"{screen_width}x{bar_height}+0+{web_height}")
root.configure(bg="#111111")

btn_config = {
    "bg": "#222222",
    "fg": "white",
    "font": ("Arial", 16, "bold"),
    "bd": 0,
    "activebackground": "#444444",
    "activeforeground": "white"
}

btn_back = tk.Button(root, text=" ◀  Tilbake ", **btn_config)
btn_back.bind("<ButtonPress-1>", lambda e: go_back())
btn_back.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

btn_home = tk.Button(root, text=" 🏠  Hjem ", **btn_config)
btn_home.bind("<ButtonPress-1>", lambda e: go_home())
btn_home.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

btn_kb = tk.Button(root, text=" ⌨  Tastatur ", **btn_config)
btn_kb.bind("<ButtonPress-1>", lambda e: toggle_keyboard())
btn_kb.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

btn_rotate = tk.Button(root, text=" 🔄  Rotér ", **btn_config)
btn_rotate.bind("<ButtonPress-1>", lambda e: rotate_screen())
btn_rotate.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

# Global peker til chromium-prosess
chromium_proc = None

# Utfør rotasjon basert på det som var lagret (eller standard ved første kjøring)
apply_rotation(current_rot_idx, initial=True)

def monitor_chromium():
    if chromium_proc and chromium_proc.poll() is not None:
        root.destroy()
    else:
        root.after(1000, monitor_chromium)

root.after(1000, monitor_chromium)
root.mainloop()

if chromium_proc:
    chromium_proc.terminate()
os.system("pkill onboard")
EOF

chown kammich:kammich /home/kammich/kiosk-manager.py
chmod +x /home/kammich/kiosk-manager.py

###############################################
# 9. X11 / xinit Oppstartsskript (med DPMS)
###############################################
cat << 'EOF' > /home/kammich/kiosk-start.sh
#!/bin/bash
export DISPLAY=:0
export XAUTHORITY=$HOME/.Xauthority

if [ -z "$DBUS_SESSION_BUS_ADDRESS" ]; then
    eval $(dbus-launch --sh-syntax)
    export DBUS_SESSION_BUS_ADDRESS
    export DBUS_SESSION_BUS_PID
fi

SCREENSAVER_TIMEOUT=300

xset s off
xset -dpms
xset dpms 0 0 "$SCREENSAVER_TIMEOUT"

gsettings set org.onboard theme 'Nightshade' 2>/dev/null

exec python3 /home/kammich/kiosk-manager.py
EOF

chown kammich:kammich /home/kammich/kiosk-start.sh
chmod +x /home/kammich/kiosk-start.sh

###############################################
# 10. Systemd Kiosk-tjeneste
###############################################
echo "[*] Oppretter systemd kiosk-tjeneste..."

cat << 'EOF' > /etc/systemd/system/kammich-kiosk.service
[Unit]
Description=Kammich Python Kiosk Service
After=network.target sound.target udev.service

[Service]
User=kammich
Group=kammich
Environment=DISPLAY=:0
ExecStart=/usr/bin/startx /home/kammich/kiosk-start.sh -- :0 vt1 -nocursor
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

###############################################
# 11. Aktiver og start
###############################################
echo "[*] Aktiverer og starter systemd-tjenesten..."
systemctl daemon-reload
systemctl disable getty@tty1.service 2>/dev/null
systemctl enable kammich-kiosk.service
systemctl restart kammich-kiosk.service
udevadm control --reload-rules && udevadm trigger

echo "[+] Alt er klart! Kiosken husker nå rotasjonen i /var/lib/kammich/rotation.idx."