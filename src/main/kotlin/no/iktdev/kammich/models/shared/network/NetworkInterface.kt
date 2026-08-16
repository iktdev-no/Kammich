package no.iktdev.kammich.models.shared.network

abstract class NetworkInterface {
    abstract val interfaceName: String
    abstract val macAdress: String
    abstract val type: NetworkInterfaceType
    abstract val mode: NetworkInterfaceMode
    abstract fun withMode(newMode: NetworkInterfaceMode): NetworkInterface
}



data class WirelessNetworkInterface(
    override val interfaceName: String,
    override val macAdress: String,
    override val mode: NetworkInterfaceMode = NetworkInterfaceMode.Idle,
    val caps: Set<WirelessNetworkInterfaceCapability> = emptySet(),
) : NetworkInterface() {
    override val type: NetworkInterfaceType = NetworkInterfaceType.Wifi
    override fun withMode(newMode: NetworkInterfaceMode) = this.copy(mode = newMode)
}

data class EthernetNetworkInterface(
    override val interfaceName: String,
    override val macAdress: String,
    override val mode: NetworkInterfaceMode = NetworkInterfaceMode.Idle
) : NetworkInterface() {
    override val type: NetworkInterfaceType = NetworkInterfaceType.Ethernet

    override fun withMode(newMode: NetworkInterfaceMode) = this.copy(mode = newMode)
}

enum class NetworkInterfaceType {
    Ethernet,
    Wifi
}

enum class NetworkInterfaceMode {
    External,
    Tether, // Fallback
    Client,
    Idle,
}

enum class WirelessNetworkInterfaceCapability {
    STA,
    AP,
    Concurrent,
    Concurrent_Restricted_Same_Channel
}