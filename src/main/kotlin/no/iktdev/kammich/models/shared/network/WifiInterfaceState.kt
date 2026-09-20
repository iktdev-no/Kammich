package no.iktdev.kammich.models.shared.network

import no.iktdev.kammich.models.internal.network.NmCliDeviceState

data class WifiInterfaceState(
    val ifName: String,
    val state: WifiInterfaceStateType,
    val operatingMode: NetworkInterfaceMode,
    val deviceState: NmCliDeviceState? = null,
    val network: WifiNetwork? = null,
    val ipv4: String? = null,
    val error: WifiInterfaceErrorType? = null
)

enum class WifiInterfaceStateType {
    Acquired,
    Connecting,
    Connected,
    Disconnecting,
    Disconnected,

    Starting,
    Tethering,
    Stopping,

    Idle
}

enum class WifiInterfaceErrorType {
    ClientWrongPassword,
    ClientNetworkNotFound,
    TetherDeviceNotFound,
    TetherStartFailed,
    TetherStopFailed,
    TetherPasswordTooShort,
    TetherInvalidSettings,
    Unknown
}