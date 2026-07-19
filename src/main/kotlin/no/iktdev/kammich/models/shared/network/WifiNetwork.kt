package no.iktdev.kammich.models.shared.network

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
    val hwMode: WifiNetworkHardwareMode // "g" eller "a"
)

enum class WifiNetworkHardwareMode {
    a, g
}

data class WifiNetworkScan(
    val name: String,
    val state: InterfaceActiveState,
    val networks: List<WifiNetwork>
)

data class WifiNetworkConnection(
    val name: String,
    val state: InterfaceActiveState,
    val network: WifiNetwork? = null
)

data class WifiNetworkTether(
    val name: String,
    val state: WirelessTetheringState,
    val network: WifiNetwork? = null
)