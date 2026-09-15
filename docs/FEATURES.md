# Kammich Features

Kammich is designed to make a dedicated camera-to-Immich workflow as automated as possible while keeping the user in control of what happens to their files.

## Camera Management

### Automatic camera detection

Kammich listens for device connection events and automatically detects supported cameras when they are connected.

Camera communication is currently handled through **gphoto2** using PTP/MTP.

When opening a connected camera, Kammich can display information such as:

- Camera model
- Camera battery level
- Import state

### Camera ownership

A user can claim ownership of a camera.

Once a camera is owned:

- Imports from that camera are automatically associated with the owner's Immich upload jobs.
- If the device has only one configured user, camera imports can be associated automatically without requiring the user to select an owner.
- Ownership can currently **not** be removed or reset.

This allows a frequently used camera to be connected and imported without having to configure the destination every time.

---

## Import & Upload

### Batch importing

Kammich imports multiple files from a camera as a batch rather than requiring individual uploads.

The import process handles:

```text
Camera
   ↓
Import
   ↓
Local file
   ↓
Hash
   ↓
Immich upload
   ↓
Verification
   ↓
Complete
````

### Upload jobs

Uploads are tracked as individual jobs.

Users can:

* View upload progress
* Reset failed uploads
* Retry failed uploads
* Monitor the state of an upload

Failed uploads do not require the entire import to be started again.

### Upload verification

Kammich keeps track of files that have been successfully uploaded and verified.

Files are verified against the SHA-1 information provided by Immich before they are considered safely uploaded.

This verification is also used as a safety requirement for source-file deletion.

---

## Camera File Deletion

Deleting files from the camera is **disabled by default**.

When enabled, Kammich will only delete source files after the corresponding files have been successfully uploaded and verified in Immich.

The intended flow is:

```text
Camera
   ↓
Import
   ↓
Upload to Immich
   ↓
Verify upload
   ↓
File is safely stored in Immich
   ↓
Delete from camera
```

Kammich does not treat an upload request alone as sufficient confirmation that a file can be deleted.

---

## Immich Integration

Kammich connects directly to the user's Immich server.

During login, Kammich creates an API key with the permissions required for its operation and stores the key locally in its own database.

Kammich does not require a separate Kammich backend or cloud service.

The only external service Kammich currently contacts outside the configured Immich server is GitHub for checking and downloading new Kammich releases. (Download and update requires user action)

### Multiple users

Multiple Immich users can be configured on the same Kammich device.

Users can select which account an import belongs to, while camera ownership can be used to automate this process.

### Storage information

Kammich can display the amount of storage currently used by the user's Immich library.

---

## Albums

Kammich can create Immich albums and automatically add uploaded files to them.

Albums can be configured with a date range defining which files should be included.

Kammich can determine the relevant date using:

* EXIF date
* Import date
* File date

Album processing is performed after the relevant files have finished uploading.

This allows a camera import to be automatically organized into an Immich album based on when the photos were taken or imported.

Deleting an album in Kammich does **not** delete an album in Immich 

---

## Network Management

### Wi-Fi

Wi-Fi networks can be selected and configured directly through the Kammich UI.

An on-screen keyboard is available through the kiosk interface, allowing the device to be configured without requiring a physical keyboard.

### Ethernet fallback

Kammich monitors Ethernet connectivity and DHCP.

If an Ethernet connection does not receive a DHCP address within approximately 90 seconds, Kammich can enter its configured Ethernet fallback/host mode.

The fallback state is reset when the Ethernet connection is disconnected.

### Captive portal

Kammich can detect captive portals.

When a captive portal is detected, Kammich can open the detected redirect URL as an overlay in the kiosk UI so that the user can authenticate to the network without leaving Kammich.

---

## Tailscale

Kammich provides information about the local Tailscale state.

The UI can display information including:

* Online status
* MagicDNS status
* Netcheck information
* Serve status
* Basic Tailscale information for the device

Tailscale itself is not managed by a Kammich server.

Kammich can be accessed remotely through Tailscale when configured by the user.

---

## Kiosk Interface

Kammich is designed to operate as a dedicated kiosk appliance.

The kiosk environment uses [pyKiosk](https://github.com/iktdev-no/pykiosk) together with X11 and Openbox.

The kiosk provides:

* Full-screen Kammich UI
* On-screen keyboard
* Captive portal overlay
* Automatic startup
* Automatic recovery of the kiosk service

No traditional desktop environment is required.

---

## System Management

Basic system controls are available through the Kammich web UI.

Users can:

* Restart the machine
* Shut down the machine
* View system/network status
* View local storage usage

Kammich also integrates with Linux device and system services for camera detection and removable-device handling.

---

