package no.iktdev.kammich.models.shared.network

enum class WifiTetheringState {
    IDLE,
    STARTING,
    RUNNING,
    ALIGNING,
    RUNNING_ALIGNED,
    ERROR
}

data class WifiTethering(
    val iface: WifiTetherInterface,
    val state: WifiTetheringState,
    val network: WifiTetheringNetwork? = null
)

data class WifiTetheringNetwork(
    val ssid: String,
    val channel: Int,
    val frequencyMhz: Int,
    val alignedToSSID: String? = null,
    val isAligned: Boolean
)

enum class WifiSecurityType(val label: String) {
    NONE("None"),
    WPA2("WPA2-Personal"),
    WPA3("WPA3-Personal");
}


data class WifiTetherInterface(
    val name: String,
    val deviceId: String,
    val enabled: Boolean,
    val supportsAp: Boolean,
    val supportsApAndStationSimultaneously: Boolean
)

data class WifiTetherSetting(
    val ssid: String,
    val password: String,
    val security: WifiSecurityType = WifiSecurityType.WPA2
)