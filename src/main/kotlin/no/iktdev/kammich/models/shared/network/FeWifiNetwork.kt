package no.iktdev.kammich.models.shared.network

// En strømlinjeformet versjon for nettverkslisten i UI-et
data class FeWifiNetwork(
    val ssid: String,
    val signalPercent: Int,
    val isSecure: Boolean,
    val bssid: String,
    val securityType: String
)

// En ren og ferdigtygget status for nettverkskortet
data class FeWifiInterface(
    val name: String,
    val supportsAp: Boolean,
    val supportsSimultaneousApSta: Boolean
)

enum class WifiActivityState {
    IDLE,
    SCANNING,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR
}


data class WifiInterfaceInfo(
    val interfaceName: String,
    val hardwareName: String,
    val supportsAp: Boolean,
    val supportsApAndStationSimultaneously: Boolean
)

enum class ConnectionStatus {
    CONNECTED,
    CAPTIVE_PORTAL,
    FAILED,
    DISCONNECTED
}

data class ConnectionResult(
    val success: Boolean,
    val message: String,
    val status: ConnectionStatus
)