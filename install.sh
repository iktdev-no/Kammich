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
    echo "Kunne ikke identifisere pakkebehandler. Installer gphoto2, smartmontools og udisks2 manuelt."
    exit 1
fi

# 2. Installer dependencies
echo "Installerer systemavhengigheter..."
$UPDATE_CMD
$PKG_MGR gphoto2 smartmontools udisks2

# 3. Opprett Udev-regler
echo "Konfigurerer udev-regler..."
cat <<EOF > /etc/udev/rules.d/99-kammich.rules
ACTION=="add", SUBSYSTEMS=="usb", KERNEL=="sd[a-z][0-9]", SYMLINK+="removable/%k", RUN+="/usr/bin/mkdir -p /media/removable/%k", RUN+="/usr/bin/mount /dev/%k /media/removable/%k"
ACTION=="remove", SUBSYSTEMS=="usb", KERNEL=="sd[a-z][0-9]", RUN+="/usr/bin/umount /media/removable/%k", RUN+="/usr/bin/rmdir /media/removable/%k"
EOF

# 4. Polkit-konfigurasjon for passordfri tilgang
# Dette gir brukeren din tilgang til å montere disker og kjøre smartctl uten sudo
USER_NAME=${SUDO_USER:-$USER}
echo "Konfigurerer Polkit for bruker: $USER_NAME..."

cat <<EOF > /etc/polkit-1/rules.d/99-kammich.rules
polkit.addRule(function(action, subject) {
    if ((action.id == "org.freedesktop.udisks2.filesystem-mount" ||
         action.id == "org.freedesktop.udisks2.filesystem-mount-system") &&
        subject.user == "$USER_NAME") {
        return polkit.Result.YES;
    }
});
EOF

# 5. sudoers-konfigurasjon for smartctl
# Gir deg lov til å kjøre smartctl uten passord
echo "$USER_NAME ALL=(ALL) NOPASSWD: /usr/sbin/smartctl" > /etc/sudoers.d/kammich

# 6. Avslutt
udevadm control --reload-rules && udevadm trigger
echo "--- Installasjon fullført ---"
echo "Kammich-miljøet er nå klargjort."