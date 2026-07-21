#!/bin/bash

# Sjekk at skriptet kjøres som root
if [[ $EUID -ne 0 ]]; then
    echo "[-] Dette skriptet må kjøres som root (sudo)."
    exit 1
fi

TARGET_USER="kammich"
MOUNT_ROOT="/run/kammich/removable"
STATE_ROOT="/run/kammich"

echo "[+] Starter oppsett av robust Python-kiosk og systemd-tjeneste..."

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
# 3. Installer avhengigheter (Xorg, Onboard, Python3, Chromium)
###############################################
echo "[*] Installerer pakker (Xorg, Onboard, Python3, Chromium)..."
apt-get update
apt-get install -y \
    gphoto2 smartmontools hdparm openjdk-21-jdk rfkill jc network-manager \
    xserver-xorg x11-xserver-utils xinit onboard python3-tk xdotool chromium chromium-common dbus-x11

# Gi bruker rettighet til å starte X uten passord
chmod u+s /usr/lib/xorg/Xorg 2>/dev/null || chmod u+s /usr/bin/Xorg 2>/dev/null

###############################################
# 4. Deaktiver Hibernate / Sleep og Network-wait-online
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
# 5. Setup Base-struktur (USB Mount)
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
# 8. Opprett komplett Python Kiosk Manager & Navbar (med EDID-støtte)
###############################################
echo "[*] Oppretter Python-basert kiosk- og navigasjonsstyring..."

cat << 'EOF' > /home/kammich/kiosk-manager.py
import tkinter as tk
import subprocess
import os
import time

def go_back():
    subprocess.run(["xdotool", "key", "Alt+Left"])

def go_home():
    subprocess.run(["xdotool", "key", "ctrl+l"])
    time.sleep(0.1)
    subprocess.run(["xdotool", "type", "https://iktdev.no"])
    subprocess.run(["xdotool", "key", "Return"])

def toggle_keyboard():
    res = subprocess.run(["pgrep", "onboard"], capture_output=True)
    if res.returncode != 0:
        subprocess.Popen(["onboard"])
    else:
        os.system("pkill onboard")

# Start Onboard i bakgrunnen
subprocess.Popen(["onboard"])

# Start Tkinter og hent ekte oppløsning fra Xorgs EDID-lesing
root = tk.Tk()
root.overrideredirect(True)
root.attributes("-topmost", True)

screen_width = root.winfo_screenwidth()
screen_height = root.winfo_screenheight()

bar_height = 50
web_height = screen_height - bar_height

# Plasser navbar nederst med full bredde og sørg for at den ligger øverst
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

btn_back = tk.Button(root, text=" ◀  Tilbake ", command=go_back, **btn_config)
btn_back.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

btn_home = tk.Button(root, text=" 🏠  Hjem ", command=go_home, **btn_config)
btn_home.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

btn_kb = tk.Button(root, text=" ⌨  Tastatur ", command=toggle_keyboard, **btn_config)
btn_kb.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

# Start Chromium i ekte kiosk-modus
chromium_proc = subprocess.Popen([
    "chromium",
    "--kiosk",
    f"--window-size={screen_width},{web_height}",
    "--window-position=0,0",
    "--no-first-run",
    "--no-default-browser-check",
    "--disable-infobars",
    "--disable-session-crashed-bubble",
    "--disable-dev-shm-usage",
    "--password-store=basic",
    "--user-data-dir=/home/kammich/.config/chromium",
    "https://iktdev.no"
])

def monitor_chromium():
    if chromium_proc.poll() is not None:
        root.destroy()
    else:
        root.after(1000, monitor_chromium)

root.after(1000, monitor_chromium)
root.mainloop()

chromium_proc.terminate()
os.system("pkill onboard")
EOF

chown kammich:kammich /home/kammich/kiosk-manager.py
chmod +x /home/kammich/kiosk-manager.py

###############################################
# 9. Oppstartsskript for X11 / xinit (med justerbar skjermsparer)
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

# =========================================================================
# VARIABEL FOR SKJERMSTIDSAVBRUDD (DPMS)
# Setter tid i sekunder før skjermen skrur av baklyset (for å hindre innbrenning).
# 300 sekunder = 5 minutter. Sett til 0 for å deaktivere helt under testing.
# =========================================================================
SCREENSAVER_TIMEOUT=300

xset s off
xset -dpms
xset dpms 0 0 "$SCREENSAVER_TIMEOUT"

# Sett et pent og moderne utseende på Onboard-tastaturet
gsettings set org.onboard theme 'Nightshade' 2>/dev/null

# Sett en fast START-posisjon og størrelse (uten å låse den fast for alltid)
# Beregner automatisk en fin plassering sentrert nederst over navbar
screen_w=$(xdpyinfo | awk '/dimensions/{print $2}' | cut -d'x' -f1)
screen_h=$(xdpyinfo | awk '/dimensions/{print $2}' | cut -d'x' -f2)
if [ -n "$screen_w" ] && [ -n "$screen_h" ]; then
    kb_w=$(( screen_w * 4 / 5 )) # 80% av skjermbredden
    kb_h=250
    kb_x=$(( (screen_w - kb_w) / 2 ))
    kb_y=$(( screen_h - kb_h - 55 ))
    gsettings set org.onboard.window.landscape x "$kb_x" 2>/dev/null
    gsettings set org.onboard.window.landscape y "$kb_y" 2>/dev/null
    gsettings set org.onboard.window.landscape width "$kb_w" 2>/dev/null
    gsettings set org.onboard.window.landscape height "$kb_h" 2>/dev/null
fi

# Start Python Kiosk Manager
exec python3 /home/kammich/kiosk-manager.py
EOF

chown kammich:kammich /home/kammich/kiosk-start.sh
chmod +x /home/kammich/kiosk-start.sh

###############################################
# 10. Opprett Systemd-tjeneste for kiosken
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
ExecStart=/usr/bin/startx /home/kammich/kiosk-start.sh -- :0 vt1
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

###############################################
# 11. Aktiver alt og start tjenesten med en gang
###############################################
echo "[*] Aktiverer og starter systemd-tjenesten umiddelbart..."
systemctl daemon-reload
systemctl disable getty@tty1.service 2>/dev/null
systemctl enable kammich-kiosk.service
systemctl restart kammich-kiosk.service

echo "[+] Installasjon og oppstart fullført!"