package no.iktdev.kammich.models.internal.network

data class NmCliDevice(
    val ifName: String,
    val ifType: NmCliDeviceType,
    val state: NmCliDeviceState,
    val isExternal: Boolean,
)

enum class NmCliDeviceType(val org: String) {
    Loopback("loopback"),
    Wifi("wifi"),
    Wifi_p2p("wifi-p2p"), // Justert til bindestrek hvis nmcli returnerer det
    Ethernet("ethernet"),
    Unknown("unknown");

    companion object {
        fun findBy(name: String): NmCliDeviceType {
            return entries.find { it.org.equals(name, ignoreCase = true) } ?: Unknown
        }
    }
}

enum class NmCliDeviceState(val org: String) {
    Connected("connected"),
    Connecting("connecting"),
    Disconnecting("disconnecting"),
    Disconnected("disconnected"),
    Unmanaged("unmanaged"),
    Unavailable( "unavailable"),
    Suspending( "suspending"),
    Suspended( "suspended"),
    Unknown("unknown");

    companion object {
        fun findBy(name: String): NmCliDeviceState {
            return NmCliDeviceState.entries.find { it.org.equals(name, ignoreCase = true) } ?: NmCliDeviceState.Unknown
        }
    }
}