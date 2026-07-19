package no.iktdev.kammich.models.shared.network

data class WirelessInterface(
    val name: String,
    val address: String,
    val isAvailable: Boolean = false, // This will be defined when returning state
    val operatingState: WirelessOperatingState = WirelessOperatingState.Idle,
    val search: WirelessNetworkSearch? = null,
    val connection: WirelessConnection? = null,
    val tethering: WirelessTethering? = null,
)

data class WirelessNetworkSearch(
    val networks: List<WifiNetwork>,
    val lastSearched: String
)

data class WirelessConnection(
    val state: InterfaceActiveState = InterfaceActiveState.Idle,
    val network: WifiNetwork? = null,
)

data class WirelessTethering(
    val state: WirelessTetheringState = WirelessTetheringState.Idle,
    val network: WifiNetwork? = null,
)

enum class WirelessTetheringState {
    Idle,
    Broadcasting
}

enum class WirelessOperatingState {
    AP,
    STA,
    Idle
}