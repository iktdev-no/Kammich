## Requirements

### Required

- Ubuntu 26.04 LTS Server
- Minimum 4 GB RAM
- Minimum 64 GB internal storage (SSD/NVMe)
  - HDD is not tested
- Wi-Fi or Ethernet connectivity

### Recommended

- 1× Ethernet
- 1× Wi-Fi
- [Tailscale](https://tailscale.com/) for remote access

Ethernet and Wi-Fi are both recommended so the device remains reachable through an alternative network connection if one becomes unavailable.

Tailscale is recommended if you want to access Kammich when you are away from your home network. Kammich's web interface is available on port `8080`.

---

## Installation

The easiest way to install Kammich is using the installer:

```bash
bash -c "$(wget -qO- https://raw.githubusercontent.com/iktdev-no/Kammich/refs/heads/master/install.sh)"
````

### For the brave

You can also install Kammich manually without using the installer.

The installer modifies system-wide configuration, installs system packages, creates systemd services and configures the machine for the Kammich kiosk. If you're installing Kammich on an existing desktop environment, **you probably don't want to use the installer**.

Manual installation allows you to configure the system yourself without potentially messing with your existing environment.

A Java JAR file is available under [Releases](https://github.com/iktdev-no/Kammich/releases), which is probably all you need to get Kammich running manually, apart from the required system configuration and udev rules for removable devices.

---

## Remote access

Installing Tailscale is recommended if you want to access Kammich remotely.

Once connected to your tailnet, the Kammich web interface is available on:

```text
http://<kammich-ip>:8080
```

This lets you access Kammich even when you're not connected to the same Wi-Fi or LAN.
