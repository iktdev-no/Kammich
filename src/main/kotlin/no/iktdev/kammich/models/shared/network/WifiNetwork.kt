package no.iktdev.kammich.models.shared.network



data class WifiNetwork(
    val inUse: Boolean,
    val isActive: Boolean,
    val ssid: String,
    val isHidden: Boolean,
    val signalPercent: Int,
    val isSecure: Boolean,
    val bssid: String,
    val securityType: String,
    val interfaceName: String,
    val channel: Int?,
    val bandwidthMhz: Int = 0,
    val frequencyMhz: Int = 0,
    val hwMode: WifiNetworkHardwareMode // "g" eller "a"
)

enum class WifiNetworkHardwareMode {
    a, g
}