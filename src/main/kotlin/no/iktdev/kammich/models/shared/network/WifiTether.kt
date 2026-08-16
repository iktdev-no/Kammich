package no.iktdev.kammich.models.shared.network



data class WifiTether(val ifName: String, val state: WirelessTetheringState = WirelessTetheringState.Idle, val network: WifiNetwork?, val error: WirelessTetheringError? = null)

enum class WirelessTetheringState {
    Idle,
    Acquired,
    Starting,
    Tethering,
    Stopping
}


enum class WirelessTetheringError {
    Unknown,
    DeviceNotFound,
    StartFailed,
    StopFailed,
    PasswordTooShort,
    InvalidSettings
}