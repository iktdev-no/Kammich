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

enum class NmCliDeviceState(val code: Int, val org: String) {
    Unmanaged(10, "unmanaged"),
    Unavailable(20, "unavailable"),
    Disconnected(30, "disconnected"),
    Prepare(40, "prepare"),
    Config(50, "config"),
    NeedAuth(60, "need-auth"),
    IpConfig(70, "ip-config"),
    IpCheck(80, "ip-check"),
    Secondaries(90, "secondaries"),
    Activated(100, "activated"),
    Deactivating(110, "deactivating"),
    Failed(120, "failed"),
    Unknown(-1, "unknown");

    companion object {
        fun findByCode(code: Int): NmCliDeviceState =
            entries.find { it.code == code } ?: Unknown

        fun findByName(name: String): NmCliDeviceState =
            entries.find { it.org.equals(name, ignoreCase = true) } ?: Unknown
    }
}