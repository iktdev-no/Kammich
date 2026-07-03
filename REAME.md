

System dependencies
gphoto2
smartmontools


# Lag en symbolsk lenke for alle USB-mass-storage enheter
ACTION=="add", SUBSYSTEMS=="usb", KERNEL=="sd[a-z][0-9]", SYMLINK+="removable/%k", RUN+="/usr/bin/mkdir -p /media/removable/%k", RUN+="/usr/bin/mount /dev/%k /media/removable/%k"
ACTION=="remove", SUBSYSTEMS=="usb", KERNEL=="sd[a-z][0-9]", RUN+="/usr/bin/umount /media/removable/%k", RUN+="/usr/bin/rmdir /media/removable/%k"