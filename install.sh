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

echo "[+] Starter komplett og robust oppsett av Kammich Kiosk (med pywebview & openbox)..."

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

    apt-get purge -y lightdm lxde-core lxde lxde-common touchegg matchbox-keyboard 2>/dev/null
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

mkdir -p /home/$TARGET_USER/logs
chown -R "$TARGET_USER:$TARGET_USER" /home/$TARGET_USER
chmod 755 /home/$TARGET_USER
usermod -aG input,tty,audio,video,render "$TARGET_USER"

USER_ID=$(id -u "$TARGET_USER")
GROUP_ID=$(id -g "$TARGET_USER")

systemctl daemon-reload

###############################################
# 3. Installer avhengigheter (Inkludert openbox)
###############################################
echo "[*] Installerer systemavhengigheter..."
apt-get update
apt-get install -y \
    gphoto2 smartmontools hdparm openjdk-21-jdk rfkill jc network-manager iw \
    xserver-xorg x11-xserver-utils xinit openbox onboard python3-tk xdotool dbus-x11 unclutter xinput \
    python3-dev python3-venv python3-pip python3-gi python3-gi-cairo \
    python3-cairo gir1.2-gtk-3.0 gir1.2-webkit2-4.1

###############################################
# 3.1 Sett opp Python Virtual Environment for pywebview
###############################################
echo "[*] Setter opp Python Virtual Environment (venv) for kammich..."
VENV_DIR="/home/$TARGET_USER/kiosk-env"

# Fjern gammel venv hvis den feilet tidligere
rm -rf "$VENV_DIR"

# Opprett venv med lenke til systemets Python-pakker (viktig for GTK/WebKit)
sudo -u "$TARGET_USER" python3 -m venv --system-site-packages "$VENV_DIR"

# Installer pywebview og Pillow i venv
sudo -u "$TARGET_USER" "$VENV_DIR/bin/pip" install --upgrade pip
sudo -u "$TARGET_USER" "$VENV_DIR/bin/pip" install pywebview Pillow

###############################################
# 3.2 Sett opp Openbox konfigurasjon (Tving Onboard øverst)
###############################################
echo "[*] Konfigurerer Openbox regler for Onboard..."
mkdir -p /home/$TARGET_USER/.config/openbox
cat << 'EOF' > /home/$TARGET_USER/.config/openbox/rc.xml
<?xml version="1.0" encoding="UTF-8"?>
<openbox_config xmlns="http://icculus.org/openbox/rdc" xmlns:xi="http://www.w3.org/2001/XInclude">
  <applications>
    <application name="onboard" class="Onboard">
      <layer>above</layer>
      <skip_pager>yes</skip_pager>
      <skip_taskbar>yes</skip_taskbar>
      <focus>no</focus>
      <decor>no</decor>
    </application>
  </applications>
</openbox_config>
EOF
chown -R "$TARGET_USER:$TARGET_USER" /home/$TARGET_USER/.config

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
echo "[*] Oppretter persistert lagringsrot..."
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
# 7. Sudoers & Network Forwarding
###############################################
echo "[*] Konfigurerer sudoers..."

cat << EOF > /etc/sudoers.d/kammich
Cmnd_Alias KAMMICH_ADMIN = /usr/sbin/smartctl, /usr/local/bin/kammich-eject, /usr/bin/xrandr, /usr/bin/xinput, /usr/bin/systemctl poweroff, /usr/bin/systemctl reboot
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
# 8. Kiosk Manager (Ren pywebview)
###############################################
cat <<'EOF' > /home/kammich/kiosk-manager.py
#!/usr/bin/env python3

import tkinter as tk
import subprocess
import os
import re
import time
import base64
from PIL import ImageTk


# ==========================================
# KONFIGURASJON
# ==========================================

START_URL = "http://192.168.2.20:5173"

ROTATION_FILE = "/var/lib/kammich/rotation.idx"

BAR_HEIGHT = 50

# Høyden på Onboard.
# Endre denne hvis du ønsker større/mindre tastatur.
KEYBOARD_HEIGHT = 300


rotations = [
    ("normal",   "1 0 0 0 1 0 0 0 1"),
    ("right",    "0 1 0 -1 0 1 0 0 1"),
    ("inverted", "-1 0 1 0 -1 1 0 0 1"),
    ("left",     "0 -1 1 1 0 0 0 0 1")
]


# ==========================================
# LAST INN LAGRET ROTASJON
# ==========================================

current_rot_idx = 0

if os.path.exists(ROTATION_FILE):
    try:
        with open(ROTATION_FILE, "r") as f:
            current_rot_idx = int(f.read().strip())

        if current_rot_idx < 0 or current_rot_idx >= len(rotations):
            current_rot_idx = 0

    except Exception:
        current_rot_idx = 0


browser_process = None


# ==========================================
# SKJERM / LAYOUT
# ==========================================

def get_output_name():
    result = subprocess.run(
        ["xrandr", "--query"],
        capture_output=True,
        text=True
    )

    for line in result.stdout.splitlines():
        if " connected" in line:
            return line.split()[0]

    return None


def get_screen_geometry():
    """
    Get the actual active XRandR geometry.

    Do not rely on Tk's screen geometry here. After xrandr rotation,
    Tk can temporarily report the old dimensions.
    """
    result = subprocess.run(
        ["xrandr", "--query"],
        capture_output=True,
        text=True
    )

    for line in result.stdout.splitlines():
        if " connected" not in line:
            continue

        # Example:
        # HDMI-1 connected primary 1920x1080+0+0
        match = re.search(r"\s(\d+)x(\d+)\+\-?\d+\+\-?\d+", line)
        if match:
            return int(match.group(1)), int(match.group(2))

    # Fallback to Tk.
    root.update_idletasks()
    return root.winfo_screenwidth(), root.winfo_screenheight()


def get_layout_geometry():
    width, height = get_screen_geometry()

    # The bottom bar is always 50 px high.
    web_height = max(1, height - BAR_HEIGHT)

    # Keyboard is an overlay. It must never extend outside the screen.
    keyboard_height = min(
        KEYBOARD_HEIGHT,
        web_height
    )

    return {
        "width": width,
        "height": height,

        # WebView occupies the complete available area above the bar.
        "web_x": 0,
        "web_y": 0,
        "web_width": width,
        "web_height": web_height,

        # Onboard is an overlay directly above the bottom bar.
        "keyboard_x": 0,
        "keyboard_y": height - BAR_HEIGHT - keyboard_height,
        "keyboard_width": width,
        "keyboard_height": keyboard_height,

        # Bottom bar.
        "bar_x": 0,
        "bar_y": height - BAR_HEIGHT,
        "bar_width": width,
        "bar_height": BAR_HEIGHT,
    }


# ==========================================
# ONBOARD
# ==========================================

keyboard_visible = False


def is_keyboard_running():
    result = subprocess.run(
        ["pgrep", "-x", "onboard"],
        capture_output=True
    )

    return result.returncode == 0


def stop_keyboard():
    global keyboard_visible

    subprocess.run(
        ["pkill", "-x", "onboard"],
        capture_output=True
    )

    keyboard_visible = False


def start_keyboard():
    global keyboard_visible

    layout = get_layout_geometry()

    if layout["keyboard_height"] <= 0:
        return

    # Kill any old instance so its saved geometry cannot interfere.
    subprocess.run(
        ["pkill", "-x", "onboard"],
        capture_output=True
    )

    time.sleep(0.15)

    subprocess.Popen([
        "onboard",
        "-x", str(layout["keyboard_x"]),
        "-y", str(layout["keyboard_y"]),
        "-s",
        f'{layout["keyboard_width"]}x{layout["keyboard_height"]}'
    ])

    keyboard_visible = True


def toggle_keyboard():
    global keyboard_visible

    if keyboard_visible or is_keyboard_running():
        stop_keyboard()
    else:
        start_keyboard()


# ==========================================
# BROWSER
# ==========================================

def stop_browser():
    global browser_process

    if browser_process is None:
        return

    try:
        browser_process.terminate()
        browser_process.wait(timeout=2)
    except Exception:
        try:
            browser_process.kill()
        except Exception:
            pass

    browser_process = None


def start_browser():
    global browser_process

    stop_browser()

    layout = get_layout_geometry()

    env = os.environ.copy()
    env["WEBKIT_DISABLE_COMPOSITING_MODE"] = "0"

    cmd = [
        "/home/kammich/kiosk-env/bin/python3",
        "-c",
        f"""
import webview

webview.create_window(
    'Kammich Kiosk',
    '{START_URL}',
    width={layout["web_width"]},
    height={layout["web_height"]},
    x={layout["web_x"]},
    y={layout["web_y"]},
    frameless=True,
    easy_drag=False,
    resizable=False,
    background_color='#111111'
)

webview.start()
"""
    ]

    browser_process = subprocess.Popen(
        cmd,
        env=env
    )


def reload_app():
    # Restart only the browser.
    # Keyboard state is intentionally left untouched.
    start_browser()


# ==========================================
# ROTASJON
# ==========================================

def apply_rotation(idx):
    global current_rot_idx

    rot_name, matrix = rotations[idx]

    output = get_output_name()

    if not output:
        print("Fant ingen tilkoblet skjerm.")
        return

    # Preserve the user's keyboard state.
    was_keyboard_visible = keyboard_visible or is_keyboard_running()

    # Nothing should be moving while XRandR changes geometry.
    if was_keyboard_visible:
        stop_keyboard()

    stop_browser()

    result = subprocess.run([
        "sudo",
        "xrandr",
        "--output",
        output,
        "--rotate",
        rot_name
    ])

    if result.returncode != 0:
        print(f"Kunne ikke rotere skjermen til {rot_name}.")
        start_browser()
        if was_keyboard_visible:
            start_keyboard()
        return

    # Update touch/digitizer mapping.
    xinput_res = subprocess.run(
        ["xinput", "list", "--name-only"],
        capture_output=True,
        text=True
    )

    for dev in xinput_res.stdout.splitlines():
        if any(
            k in dev.lower()
            for k in [
                "touch",
                "digitizer",
                "pen",
                "stylus",
                "ctp"
            ]
        ):
            subprocess.run([
                "sudo",
                "xinput",
                "set-prop",
                dev,
                "Coordinate Transformation Matrix"
            ] + matrix.split())

    # Give XRandR/XInput time to settle.
    time.sleep(0.8)

    layout = get_layout_geometry()

    # Keep the Tk bottom bar exactly at the bottom of the actual screen.
    root.geometry(
        f'{layout["bar_width"]}x{layout["bar_height"]}'
        f'+{layout["bar_x"]}+{layout["bar_y"]}'
    )
    root.update_idletasks()

    # Start the browser using the NEW geometry.
    start_browser()

    # Only restore Onboard if it was visible before rotation.
    if was_keyboard_visible:
        start_keyboard()


def rotate_screen():
    global current_rot_idx

    current_rot_idx = (
        current_rot_idx + 1
    ) % len(rotations)

    try:
        os.makedirs(
            os.path.dirname(ROTATION_FILE),
            exist_ok=True
        )

        with open(ROTATION_FILE, "w") as f:
            f.write(str(current_rot_idx))

    except Exception as e:
        print(f"Klarte ikke å lagre rotasjon: {e}")

    apply_rotation(current_rot_idx)


# ==========================================
# TKINTER
# ==========================================

root = tk.Tk()

root.overrideredirect(True)
root.attributes("-topmost", True)
root.focus_force()

root.configure(bg="#111111")


# ==========================================
# IKONER
# ==========================================

rotate_icon_bytes = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAEsWlUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4KPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNS41LjAiPgogPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIgogICAgeG1sbnM6ZXhpZj0iaHR0cDovL25zLmFkb2JlLmNvbS9leGlmLzEuMC8iCiAgICB4bWxuczp0aWZmPSJodHRwOi8vbnMuYWRvYmUuY29tL3RpZmYvMS4wLyIKICAgIHhtbG5zOnBob3Rvc2hvcD0iaHR0cDovL25zLmFkb2JlLmNvbS9waG90b3Nob3AvMS4wLyIKICAgIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIKICAgIHhtbG5zOnhtcE1NPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvbW0vIgogICAgeG1sbnM6c3RFdnQ9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIKICAgZXhpZjpQaXhlbFhEaW1lbnNpb249IjMyIgogICBleGlmOlBpeGVsWURpbWVuc2lvbj0iMzIiCiAgIGV4aWY6Q29sb3JTcGFjZT0iMSIKICAgdGlmZjpJbWFnZVdpZHRoPSIzMiIKICAgdGlmZjpJbWFnZUxlbmd0aD0iMzIiCiAgIHRpZmY6UmVzb2x1dGlvblVuaXQ9IjIiCiAgIHRpZmY6WFJlc29sdXRpb249IjcyLzEiCiAgIHRpZmY6WVJlc29sdXRpb249IjcyLzEiCiAgIHBob3Rvc2hvcDpDb2xvck1vZGU9IjMiCiAgIHBob3Rvc2hvcDpJQ0NQcm9maWxlPSJzUkdCIElFQzYxOTY2LTIuMSIKICAgeG1wOk1vZGlmeURhdGU9IjIwMjYtMDgtMDhUMDM6NDU6MzArMDI6MDAiCiAgIHhtcDpNZXRhZGF0YURhdGU9IjIwMjYtMDgtMDhUMDM6NDU6MzArMDI6MDAiPgogICA8eG1wTU06SGlzdG9yeT4KICAgIDxyZGY6U2VxPgogICAgIDxyZGY6bGkKICAgICAgc3RFdnQ6YWN0aW9uPSJwcm9kdWNlZCIKICAgICAgc3RFdnQ6c29mdHdhcmVBZ2VudD0iQWZmaW5pdHkgUGhvdG8gMiAyLjYuNSIKICAgICAgc3RFdnQ6d2hlbj0iMjAyNi0wOC0wOFQwMzo0NTozMCswMjowMCIvPgogICAgPC9yZGY6U2VxPgogICA8L3htcE1NOkhpc3Rvcnk+CiAgPC9yZGY6RGVzY3JpcHRpb24+CiA8L3JkZjpSREY+CjwveDp4bXBtZXRhPgo8P3hwYWNrZXQgZW5kPSJyIj8+tfO9uQAAAYBpQ0NQc1JHQiBJRUM2MTk2Ni0yLjEAACiRdZHPK0RRFMc/ZogYUSQLi5eGFfKjJjYWI4bCYuYpvzYzz3szama83nuTJltlqyix8WvBX8BWWStFpGQtS2KDnvOMGsmc0733c7/3nNO954JPTWsZu7wbMlnHikbCyvTMrFL5RJV4GQGa4pptTsRGVEra241Eil11erVKx/1rNQu6rUFZlfCgZlqO8Kjw+LJjerwp3Kil4gvCx8IdllxQ+NrTEwV+9DhZ4A+PLTU6BL56YSX5ixO/WEtZGWF5OcFMOqf93Md7SUDPTsVkbZXRgk2UCGEUxhhmiBA9DMgcopNeumRHifzu7/xJliRXk9kkj8UiSVI4dIiak+q6rIbouniavNf/v321jb7eQvVAGCoeXPelDSo34HPddd/3XffzAPz3cJYt5i/tQf+r6OtFLbgLdatwcl7UEltwugbNd2bcin9Lfhk+w4DnI6idgYZLqJ4r9OznnMNbUFfkqy5gewfaJb5u/gsNFme9rCn+aQAAAAlwSFlzAAALEwAACxMBAJqcGAAAAXdJREFUWIXtlkFKw0AYRr8R607XatttRTyB4qJbD9ALeAFBcKU3kIKX0E2FVl24VMSCV9B2aXsBdxV8LpxiGJNxmk4jqB8EksnM/14mmTDSf3IE2ABWfgK8BtzxkVfgDFgqEj7ka7ozl0iBXwH9qBJAFWjYo+qBt4EFoBJVwoLHafjgiTHxJFyBFHgnCXckelNLOAJHIfCoEo5AMl54NIkMgYsQeKJGObeEZwbGac1CYi7w4YJjjBlIqkvq26YtSddZEvOJ82dJ557aD5NIAHVJx5JKtnlX0klojb8Tk9YI7EnatpcjSQfGmGFoUWBf0qanS9MYk/1KgUXgPvElPwKrEwi0vllRjXHf1FVgjHmRtCOpa5tqkm4mkYiSCDNRsj+zzBnII/EUIpECf8slkEfCwjuJ/gPgMLeAR6IcCK+Rst+ILmHhbRdu700v4JPwwe24Cp9bvkpugQyJHnCZBZ9JUiSKgzsSp8DIwm8Lgzsiy8B64eBfkXfrw9rdARZg2gAAAABJRU5ErkJggg==")
keyboard_icon_bytes = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAEsWlUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4KPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNS41LjAiPgogPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIgogICAgeG1sbnM6ZXhpZj0iaHR0cDovL25zLmFkb2JlLmNvbS9leGlmLzEuMC8iCiAgICB4bWxuczp0aWZmPSJodHRwOi8vbnMuYWRvYmUuY29tL3RpZmYvMS4wLyIKICAgIHhtbG5zOnBob3Rvc2hvcD0iaHR0cDovL25zLmFkb2JlLmNvbS9waG90b3Nob3AvMS4wLyIKICAgIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIKICAgIHhtbG5zOnhtcE1NPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvbW0vIgogICAgeG1sbnM6c3RFdnQ9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIKICAgZXhpZjpQaXhlbFhEaW1lbnNpb249IjMyIgogICBleGlmOlBpeGVsWURpbWVuc2lvbj0iMzIiCiAgIGV4aWY6Q29sb3JTcGFjZT0iMSIKICAgdGlmZjpJbWFnZVdpZHRoPSIzMiIKICAgdGlmZjpJbWFnZUxlbmd0aD0iMzIiCiAgIHRpZmY6UmVzb2x1dGlvblVuaXQ9IjIiCiAgIHRpZmY6WFJlc29sdXRpb249IjcyLzEiCiAgIHRpZmY6WVJlc29sdXRpb249IjcyLzEiCiAgIHBob3Rvc2hvcDpDb2xvck1vZGU9IjMiCiAgIHBob3Rvc2hvcDpJQ0NQcm9maWxlPSJzUkdCIElFQzYxOTY2LTIuMSIKICAgeG1wOk1vZGlmeURhdGU9IjIwMjYtMDgtMDhUMDM6NDQ6MjQrMDI6MDAiCiAgIHhtcDpNZXRhZGF0YURhdGU9IjIwMjYtMDgtMDhUMDM6NDQ6MjQrMDI6MDAiPgogICA8eG1wTU06SGlzdG9yeT4KICAgIDxyZGY6U2VxPgogICAgIDxyZGY6bGkKICAgICAgc3RFdnQ6YWN0aW9uPSJwcm9kdWNlZCIKICAgICAgc3RFdnQ6c29mdHdhcmVBZ2VudD0iQWZmaW5pdHkgUGhvdG8gMiAyLjYuNSIKICAgICAgc3RFdnQ6d2hlbj0iMjAyNi0wOC0wOFQwMzo0NDoyNCswMjowMCIvPgogICAgPC9yZGY6U2VxPgogICA8L3htcE1NOkhpc3Rvcnk+CiAgPC9yZGY6RGVzY3JpcHRpb24+CiA8L3JkZjpSREY+CjwveDp4bXBtZXRhPgo8P3hwYWNrZXQgZW5kPSJyIj8++IFw1gAAAYBpQ0NQc1JHQiBJRUM2MTk2Ni0yLjEAACiRdZHPK0RRFMc/ZogYUSQLi5eGFfKjJjYWI4bCYuYpvzYzz3szama83nuTJltlqyix8WvBX8BWWStFpGQtS2KDnvOMGsmc0733c7/3nNO954JPTWsZu7wbMlnHikbCyvTMrFL5RJV4GQGa4pptTsRGVEra241Eil11erVKx/1rNQu6rUFZlfCgZlqO8Kjw+LJjerwp3Kil4gvCx8IdllxQ+NrTEwV+9DhZ4A+PLTU6BL56YSX5ixO/WEtZGWF5OcFMOqf93Md7SUDPTsVkbZXRgk2UCGEUxhhmiBA9DMgcopNeumRHifzu7/xJliRXk9kkj8UiSVI4dIiak+q6rIbouniavNf/v321jb7eQvVAGCoeXPelDSo34HPddd/3XffzAPz3cJYt5i/tQf+r6OtFLbgLdatwcl7UEltwugbNd2bcin9Lfhk+w4DnI6idgYZLqJ4r9OznnMNbUFfkqy5gewfaJb5u/gsNFme9rCn+aQAAAAlwSFlzAAALEwAACxMBAJqcGAAAAPtJREFUWIXtlk1uwjAQRt+gikpFKgeAg7CuOALcor1BeyI4Auw5COq6SO2GzddFnWjkGgIEhwV+m2R+lM+ejDyGQuHesdghaQBMgf6VtfbA2sy+D2ZIepX0pXzsJL0lKyDpCfgEhlfeecwOGJnZD0DPBV46ECdoTCvjwQUeo8QtsAnvk/BsY4/dt+v+8guI2ZjZHEDSAqClPUuJ9FLOLjlWgUm1E0JJ29opfAX+nQldUHrg5j1QI2mW8QiOmVe6N2/Cc3pgaScCLHMsIAu+CfcNuWP/75pyG+K1lh/HA/7G8fOJIpeSHsfB8R4Scop/VOLQ/ZVs5cULhQLALyy4ULFojsCNAAAAAElFTkSuQmCC")
restart_icon_bytes = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAEsWlUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4KPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNS41LjAiPgogPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIgogICAgeG1sbnM6ZXhpZj0iaHR0cDovL25zLmFkb2JlLmNvbS9leGlmLzEuMC8iCiAgICB4bWxuczp0aWZmPSJodHRwOi8vbnMuYWRvYmUuY29tL3RpZmYvMS4wLyIKICAgIHhtbG5zOnBob3Rvc2hvcD0iaHR0cDovL25zLmFkb2JlLmNvbS9waG90b3Nob3AvMS4wLyIKICAgIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIKICAgIHhtbG5zOnhtcE1NPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvbW0vIgogICAgeG1sbnM6c3RFdnQ9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIKICAgZXhpZjpQaXhlbFhEaW1lbnNpb249IjMyIgogICBleGlmOlBpeGVsWURpbWVuc2lvbj0iMzIiCiAgIGV4aWY6Q29sb3JTcGFjZT0iMSIKICAgdGlmZjpJbWFnZVdpZHRoPSIzMiIKICAgdGlmZjpJbWFnZUxlbmd0aD0iMzIiCiAgIHRpZmY6UmVzb2x1dGlvblVuaXQ9IjIiCiAgIHRpZmY6WFJlc29sdXRpb249IjcyLzEiCiAgIHRpZmY6WVJlc29sdXRpb249IjcyLzEiCiAgIHBob3Rvc2hvcDpDb2xvck1vZGU9IjMiCiAgIHBob3Rvc2hvcDpJQ0NQcm9maWxlPSJzUkdCIElFQzYxOTY2LTIuMSIKICAgeG1wOk1vZGlmeURhdGU9IjIwMjYtMDgtMDhUMDM6NDU6MDMrMDI6MDAiCiAgIHhtcDpNZXRhZGF0YURhdGU9IjIwMjYtMDgtMDhUMDM6NDU6MDMrMDI6MDAiPgogICA8eG1wTU06SGlzdG9yeT4KICAgIDxyZGY6U2VxPgogICAgIDxyZGY6bGkKICAgICAgc3RFdnQ6YWN0aW9uPSJwcm9kdWNlZCIKICAgICAgc3RFdnQ6c29mdHdhcmVBZ2VudD0iQWZmaW5pdHkgUGhvdG8gMiAyLjYuNSIKICAgICAgc3RFdnQ6d2hlbj0iMjAyNi0wOC0wOFQwMzo0NTowMyswMjowMCIvPgogICAgPC9yZGY6U2VxPgogICA8L3htcE1NOkhpc3Rvcnk+CiAgPC9yZGY6RGVzY3JpcHRpb24+CiA8L3JkZjpSREY+CjwveDp4bXBtZXRhPgo8P3hwYWNrZXQgZW5kPSJyIj8+tGW9jAAAAYBpQ0NQc1JHQiBJRUM2MTk2Ni0yLjEAACiRdZHPK0RRFMc/ZogYUSQLi5eGFfKjJjYWI4bCYuYpvzYzz3szama83nuTJltlqyix8WvBX8BWWStFpGQtS2KDnvOMGsmc0733c7/3nNO954JPTWsZu7wbMlnHikbCyvTMrFL5RJV4GQGa4pptTsRGVEra241Eil11erVKx/1rNQu6rUFZlfCgZlqO8Kjw+LJjerwp3Kil4gvCx8IdllxQ+NrTEwV+9DhZ4A+PLTU6BL56YSX5ixO/WEtZGWF5OcFMOqf93Md7SUDPTsVkbZXRgk2UCGEUxhhmiBA9DMgcopNeumRHifzu7/xJliRXk9kkj8UiSVI4dIiak+q6rIbouniavNf/v321jb7eQvVAGCoeXPelDSo34HPddd/3XffzAPz3cJYt5i/tQf+r6OtFLbgLdatwcl7UEltwugbNd2bcin9Lfhk+w4DnI6idgYZLqJ4r9OznnMNbUFfkqy5gewfaJb5u/gsNFme9rCn+aQAAAAlwSFlzAAALEwAACxMBAJqcGAAAAapJREFUWIXtljEvQ1EYhr+r6aBNLAzahEVCVMJQInTzC3Qm4heY/QS2ktRQE2sHERMLgxCNwaIGiUoTo8HSoQmP5dzkc92rzr3SG0nf6UvPed/36T23JxXpKYKAgVjLgatYywFiLe86gLfcqACk4yp39Q7cAwfAUhwAWh/A3p89FaC/A8QGcAjUTbmrJ2AxankGaAALQRCe/QXgUUG8AsNRAE5M0Bsw5gfh40kB+wriOGz5ugq5BhKe9cCLCEgYj6u1MABNY24B4wF7Aq9iYMJ4AZq25RlFX7Zk1zllleP7LvQFeGfVfBMWQERqAZkdAfJqvo0AoL15vw1BACNqtju/r9LeURuABzVPRgDIqbluA3Cn5pkIANMBmT8LGFJv72nYduBM5Qzami+UeTlEeVH5z239AkwBbRPQALIW3izwbLxtINfZ5R+0rb7FC+D7W/Z45sxeV1uhyk1YGrhUYS2g5Adiikvq+sV4U6EBTHAS2OW7KmpPxWd9B0hGKveArHoebVWtVT1HtfLbXMcSwhGReREpigiO42yaz91zPhKRmuM43f+73tO/1SfCqbVMj9ut5AAAAABJRU5ErkJggg==")



# NB:
# De tre base64-strengene over må erstattes med de komplette
# strengene fra originalfilen din dersom de ikke allerede
# ligger i filen du kjører.


img_refresh = ImageTk.PhotoImage(
    data=restart_icon_bytes
)

img_keyboard = ImageTk.PhotoImage(
    data=keyboard_icon_bytes
)

img_rotate = ImageTk.PhotoImage(
    data=rotate_icon_bytes
)


# ==========================================
# BOTTOM BAR
# ==========================================

btn_container = tk.Frame(
    root,
    bg="#000000",
    bd=0,
    highlightthickness=0
)

btn_container.pack(
    fill=tk.BOTH,
    expand=True,
    padx=0,
    pady=0
)


btn_config = {
    "bg": "#000000",
    "fg": "white",
    "font": ("Arial", 14, "bold"),

    # Ingen border
    "bd": 0,
    "borderwidth": 0,
    "highlightthickness": 0,
    "relief": tk.FLAT,

    "activebackground": "#444444",
    "activeforeground": "white",

    "compound": tk.LEFT,
    "padx": 10,
}


# ==========================================
# KNAPPER
# ==========================================

btn_refresh = tk.Button(
    btn_container,
    image=img_refresh,
    **btn_config
)

btn_refresh.bind(
    "<ButtonPress-1>",
    lambda e: reload_app()
)

btn_refresh.pack(
    side=tk.LEFT,
    fill=tk.BOTH,
    expand=True
)


# 70 % av 50 px = 35 px
SEPARATOR_HEIGHT = int(BAR_HEIGHT * 0.70)


separator1 = tk.Frame(
    btn_container,
    bg="white",
    width=1,
    height=SEPARATOR_HEIGHT
)

separator1.pack(
    side=tk.LEFT,
    fill=tk.Y,
    pady=(
        (BAR_HEIGHT - SEPARATOR_HEIGHT) // 2,
        (BAR_HEIGHT - SEPARATOR_HEIGHT + 1) // 2
    )
)


btn_kb = tk.Button(
    btn_container,
    image=img_keyboard,
    **btn_config
)

btn_kb.bind(
    "<ButtonPress-1>",
    lambda e: toggle_keyboard()
)

btn_kb.pack(
    side=tk.LEFT,
    fill=tk.BOTH,
    expand=True
)


separator2 = tk.Frame(
    btn_container,
    bg="white",
    width=1,
    height=SEPARATOR_HEIGHT
)

separator2.pack(
    side=tk.LEFT,
    fill=tk.Y,
    pady=(
        (BAR_HEIGHT - SEPARATOR_HEIGHT) // 2,
        (BAR_HEIGHT - SEPARATOR_HEIGHT + 1) // 2
    )
)


btn_rotate = tk.Button(
    btn_container,
    image=img_rotate,
    **btn_config
)

btn_rotate.bind(
    "<ButtonPress-1>",
    lambda e: rotate_screen()
)

btn_rotate.pack(
    side=tk.LEFT,
    fill=tk.BOTH,
    expand=True
)


# ==========================================
# START
# ==========================================

def initial_start():
    """
    Restore the saved screen rotation and start the browser.

    Onboard is deliberately NOT started here.
    It only appears after pressing the keyboard button.
    """

    # Make absolutely sure a stale Onboard instance from a previous
    # process cannot remain visible.
    stop_keyboard()

    apply_rotation(current_rot_idx)


# ==========================================
# WATCHDOG
# ==========================================

def monitor_app():
    if (
        browser_process is not None
        and browser_process.poll() is not None
    ):
        print("Browser-prosessen døde. Starter på nytt...")
        start_browser()

        # Do not start Onboard here.
        # If it was visible, it remains visible independently.

    root.after(
        1000,
        monitor_app
    )


# Start kiosk
initial_start()

root.after(
    1000,
    monitor_app
)


# ==========================================
# SHUTDOWN
# ==========================================

try:
    root.mainloop()

finally:

    stop_keyboard()

    if browser_process:
        try:
            browser_process.terminate()
        except Exception:
            pass

EOF

chown kammich:kammich /home/kammich/kiosk-manager.py
chmod +x /home/kammich/kiosk-manager.py

###############################################
# 9. X11 / xinit Oppstartsskript (Med Openbox)
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

xset +dpms
xset s off
xset dpms $SCREENSAVER_TIMEOUT $SCREENSAVER_TIMEOUT $SCREENSAVER_TIMEOUT

gsettings set org.onboard theme 'Nightshade' 2>/dev/null

# Start Openbox vindushåndterer i bakgrunnen
openbox-session &

# Gi vindushåndtereren et øyeblikk til å sette opp lagene
sleep 1

exec /home/kammich/kiosk-env/bin/python3 /home/kammich/kiosk-manager.py
EOF

chown kammich:kammich /home/kammich/kiosk-start.sh
chmod +x /home/kammich/kiosk-start.sh

###############################################
# 10. Systemd Kiosk-tjeneste
###############################################
echo "[*] Oppretter systemd kiosk-tjeneste..."

cat << 'EOF' > /etc/systemd/system/kammich-kiosk.service
[Unit]
Description=Kammich Python Kiosk Service (pywebview + openbox)
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

echo "[+] Alt er klart med Openbox, Pillow og Onboard!"