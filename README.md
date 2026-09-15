# Kammich

<p align="center">
  <img src="web/public/kammich.svg" width="200" alt="Kammich">
</p>

**A dedicated camera-to-Immich ingest station.**

Kammich turns a small, dedicated computer into an ingest station for cameras and other devices that cannot upload directly to an Immich server.

The idea is simple:

```text
Camera → Kammich → Immich
````

Kammich handles importing, batching, uploading and verifying files before optionally removing them from the source device.

---

## Features

* Camera import through **gphoto2** using PTP/MTP
* Batch importing and upload jobs
* Immich authentication with multi-user support
* Camera ownership for automatic import handling
* Upload verification before source-file deletion
* Immich album creation and automatic organization
* Wi-Fi and Ethernet management
* Ethernet DHCP fallback
* Captive portal support
* Tailscale status and network information
* Dedicated kiosk interface using [pyKiosk](https://github.com/iktdev-no/pykiosk)
* Local system and storage information

See [FEATURES.md](docs/FEATURES.md) for the full feature overview.

---

## Requirements

### Required

* Ubuntu 26.04 LTS Server
* Minimum 4 GB RAM
* Minimum 64 GB internal storage (SSD/NVMe)

    * HDD is not tested
* Wi-Fi or Ethernet connectivity

### Recommended

* 1× Ethernet
* 1× Wi-Fi
* [Tailscale](https://tailscale.com/) for remote access

Ethernet and Wi-Fi are both recommended so the device remains reachable through an alternative network connection if one becomes unavailable.

Tailscale is recommended if you want to access Kammich when you are away from your home network. Kammich's web interface is available on port `8080`.

---

## Installation

The easiest way to install Kammich is using the installer:

```bash
bash -c "$(wget -qO- https://raw.githubusercontent.com/iktdev-no/Kammich/refs/heads/master/install.sh)"
```

### For the brave

You can also install Kammich manually without using the installer.

The installer modifies system-wide configuration, installs system packages, creates systemd services and configures the machine for the Kammich kiosk. If you're installing Kammich on an existing desktop environment, **you probably don't want to use the installer**.

Manual installation allows you to configure the system yourself without potentially messing with your existing environment.

A Java JAR file is available under [Releases](https://github.com/iktdev-no/Kammich/releases), which is probably all you need to get Kammich running manually, apart from the required system configuration and udev rules for removable devices.

See [INSTALLATION.md](docs/INSTALLATION.md) for more information.

---

## Documentation

* [Features](docs/FEATURES.md) — Full feature overview
* [Architecture](docs/ARCHITECTURE.md) — System and software architecture
* [Installation](docs/INSTALLATION.md) — Installation and system configuration
* [Hardware](docs/HARDWARE.md) — Hardware requirements and enclosure

---

## Project Status

Kammich is currently a **work in progress**.

The core camera → Kammich → Immich workflow is functional, while additional hardware, networking and automation features are still being developed.

---

## License

See [LICENSE](LICENSE).


## Privacy & Connectivity

Kammich is designed to operate without a Kammich-hosted backend or cloud service.

The application communicates directly with:

1. The configured Immich server
2. GitHub, when checking for or downloading Kammich updates
3. Network services required for the user's own network configuration

There is no requirement to send photos, metadata or usage information to a separate Kammich server.

The Immich API credentials generated during login are stored locally on the Kammich device.

My privacy policy is simple:

> I don't want your data. I just want to know if my shit breaks so I can fix it.
