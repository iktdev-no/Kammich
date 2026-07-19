package no.iktdev.kammich.models.shared.network

import no.iktdev.kammich.models.internal.network.InterfaceState

data class WifiNetwork(
    val ssid: String,
    val isHidden: Boolean,
    val signalPercent: Int,
    val isSecure: Boolean,
    val bssid: String,
    val securityType: String,
    val interfaceName: String,
    val channel: Int?,
    val frequencyMhz: Int = 0,
    val hwMode: WifiNetworkHardwareMode // "g" eller "a"
)

enum class WifiNetworkHardwareMode {
    a, g
}

interface WifiNetworkInterface {
    val name: String
}

data class WifiNetworkScan(
    override val name: String,
    val state: InterfaceActiveState,
    val networks: List<WifiNetwork>
): WifiNetworkInterface

data class WifiNetworkConnection(
    override val name: String,
    val state: InterfaceActiveState,
    val network: WifiNetwork? = null
): WifiNetworkInterface

data class WifiNetworkTether(
    override val name: String,
    val state: WirelessTetheringState,
    val network: WifiNetwork? = null
): WifiNetworkInterface