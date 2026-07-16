package no.iktdev.kammich.models.shared.network

data class WifiInterfaceState(
    val interfaceName: String,
    val connectivityState: ConnectivityState,
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
    val supportsSimultaneousApSta: Boolean,
    val deviceId: String
)

enum class WifiScanState {
    IDLE,
    SCANNING,
    ERROR
}


data class WifiInterfaceInfo(
    val interfaceName: String,
    val hardwareName: String,
    val supportsAp: Boolean,
    val supportsApAndStationSimultaneously: Boolean,
    val limitation: InterfaceLimitation = InterfaceLimitation.NONE,
    val deviceId: String,
    val role: InterfaceRole
)

enum class InterfaceLimitation {
    NONE,
    CHANNEL_1_TO_1, // Krever 1:1 mellom AP-kanal og Stasjon-kanal
    NO_CONCURRENT  // Støtter ikke AP og Stasjon samtidig
}

enum class InterfaceRole {
    CLIENT, // Kan brukes til å scanne og koble til wifi
    AP,     // Kan brukes til tethering (hotspot)
    DUAL    // Fysisk kort som kan være begge deler
}


data class WifiConnectionResult(
    val success: Boolean,
    val message: String,
    val status: ConnectivityState
)