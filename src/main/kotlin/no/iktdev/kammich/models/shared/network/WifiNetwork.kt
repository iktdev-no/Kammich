package no.iktdev.kammich.models.shared.network

data class WifiInterfaceState(
    val interfaceName: String,
    val connectivityState: WifiConnectivityState,
    val network: WifiNetwork? = null,
)

data class WifiInterfaceScanState(
    val interfaceName: String,
    val scanning: WifiScanState,
    val networks: List<WifiNetwork>
)

// En strømlinjeformet versjon for nettverkslisten i UI-et
data class WifiNetwork(
    val ssid: String,
    val isHidden: Boolean,
    val signalPercent: Int,
    val isSecure: Boolean,
    val bssid: String,
    val securityType: String,
    val interfaceName: String,
    val channel: Int?,
    val frequencyMhz: Int?,
    val hwMode: String? // "g" eller "a"
)

// En ren og ferdigtygget status for nettverkskortet
data class WifiInterface(
    val name: String,
    val supportsAp: Boolean,
    val supportsSimultaneousApSta: Boolean
)

enum class WifiScanState {
    IDLE,
    SCANNING,
    ERROR
}

enum class WifiConnectivityState {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    CAPTIVE_PORTAL,
    FAILED,
    ERROR
}


data class WifiInterfaceInfo(
    val interfaceName: String,
    val hardwareName: String,
    val supportsAp: Boolean,
    val supportsApAndStationSimultaneously: Boolean
)


data class WifiConnectionResult(
    val success: Boolean,
    val message: String,
    val status: WifiConnectivityState
)