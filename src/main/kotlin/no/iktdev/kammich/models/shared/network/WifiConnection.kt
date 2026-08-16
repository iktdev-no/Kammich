package no.iktdev.kammich.models.shared.network



data class WifiConnection(
    val ifName: String,
    val state: WifiConnectionStateType,
    val network: WifiNetwork?,
    val error: WifiInterfaceClientError? = null,
)

enum class WifiInterfaceClientError {
    WrongPassword,
    NetworkNotFound,
    Unknown
}

enum class WifiConnectionStateType {
    Connecting,
    Connected,
    Disconnecting,
    Disconnected,
    Idle
}
