# Kammich Architecture

Kammich is split into a Kotlin/Spring Boot backend and a React/TypeScript frontend, with the backend acting as the bridge between the kiosk UI, cameras, Immich and the underlying Linux system.

```text
┌──────────────────────────────────────────────┐
│                   Kammich                    │
│                                              │
│  ┌───────────────────────────────────────┐   │
│  │           React / TypeScript          │   │
│  │                Kiosk UI               │   │
│  └───────────────────┬───────────────────┘   │
│                      │ HTTP / SSE            │
│  ┌───────────────────▼───────────────────┐   │
│  │          Kotlin / Spring Boot         │   │
│  │                Backend                │   │
│  └────────┬────────────┬───────────┬─────┘   │
│           │            │           │         │
│           ▼            ▼           ▼         │
│        gphoto2       Immich     Linux/System │
│        Cameras        API        Networking  │
│                                              │
└──────────────────────────────────────────────┘
           │                        │
           ▼                        ▼
        Camera                   Network
                                    │
                                    ▼
                                  Immich
```

## Frontend

The frontend is built with **React and TypeScript**.

It runs as a kiosk application and communicates with the backend over HTTP. Server-Sent Events (SSE) are used for live system and import state where applicable.

The frontend is responsible for presenting:

* Immich authentication
* User selection
* Camera/import state
* Upload progress
* Storage information
* Network status
* System status

The frontend does not directly interact with cameras or the underlying Linux system.

---

## Backend

The backend is built with **Kotlin and Spring Boot**.

It acts as the central application layer and is responsible for coordinating the different parts of Kammich.

The backend handles:

* Immich API communication
* Authentication
* Camera detection and importing
* Import and upload jobs
* Upload verification
* File hashing
* Storage information
* Network monitoring and configuration
* System and hardware operations

---

## Camera import

Camera communication is currently handled through **gphoto2**.

```text
Camera
   │
   │ PTP / MTP
   ▼
gphoto2
   │
   ▼
Kammich Backend
   │
   ├── Import files
   ├── Hash files
   └── Upload to Immich
```

Kammich currently does not use a mounted block device as the camera import mechanism.

---

## Immich

The backend communicates directly with the Immich server.

```text
Kammich Backend
      │
      │ Immich API
      ▼
Immich Server
```

The backend keeps Immich-specific operations out of the frontend. The frontend communicates with Kammich rather than directly with Immich.

---

## System integration

Kammich is designed to run directly on Linux rather than inside a conventional desktop application.

The backend interacts with the operating system for functionality such as:

* Network configuration
* Network interface monitoring
* USB/removable-device handling
* Disk health information
* Device eject
* System restart/shutdown
* Other hardware-related operations

Some privileged operations are exposed through restricted `sudoers` rules rather than running the entire backend as root.

---

## Services

Kammich is deployed as separate systemd services.

### Backend

```text
kammich-backend.service
```

Runs the Kotlin/Spring Boot application.

### Kiosk

Kammich uses [pyKiosk](https://github.com/iktdev-no/pykiosk) as its kiosk runtime.

pyKiosk is a lightweight Python-based kiosk application developed specifically to provide a controlled browser-based environment for Kammich and similar appliance-style applications.

The kiosk is started by the `kammich-kiosk.service` systemd service and runs on top of X11/Openbox.

```text
systemd
   │
   ▼
kammich-kiosk.service
   │
   ▼
X11 / Openbox
   │
   ▼
pyKiosk
   │
   ▼
Kammich Web UI
```

Using a dedicated kiosk runtime keeps the frontend isolated from the underlying desktop environment and allows Kammich to run on a minimal Ubuntu Server installation without requiring a full desktop environment.

---

## Runtime layout

Persistent application data is stored under:

```text
/var/lib/kammich
```

Runtime state and removable-device mounts are stored under:

```text
/run/kammich
```

The exact filesystem layout and installation details are documented separately in [Installation](INSTALLATION.md).

---

## Design principles

Kammich is intended to behave more like an **appliance** than a traditional desktop application.

The main principles are:

* The machine should be dedicated to Kammich.
* The backend owns system and hardware interaction.
* The frontend communicates through the backend.
* Camera files should not be deleted until the upload has been verified.
* System-level operations should use restricted privileges where possible.
* The kiosk should recover automatically if the application or system service fails.
