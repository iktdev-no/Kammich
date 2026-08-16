package no.iktdev.kammich.models.shared.network


abstract class SharedWifiInterfaceInstance(
    open val name: String,
    open val isUsable: Boolean,
    open val operatingMode: NetworkInterfaceMode,
    open val network: WifiNetwork? = null,
    open val caps: Set<WirelessNetworkInterfaceCapability> = emptySet(),
) {
    abstract val mode: InterfaceMode
}

data class WifiInterfaceClient(
    override val name: String,
    override val isUsable: Boolean,
    override val operatingMode: NetworkInterfaceMode,
    val state: WifiConnectionStateType = WifiConnectionStateType.Idle,
    override val network: WifiNetwork? = null,
    override val caps: Set<WirelessNetworkInterfaceCapability> = emptySet(),
): SharedWifiInterfaceInstance(
    name = name,
    isUsable = isUsable,
    network = network,
    caps = caps,
    operatingMode = operatingMode,
) {
    override val mode: InterfaceMode = InterfaceMode.Client
}

data class WifiInterfaceTether(
    override val name: String,
    override val isUsable: Boolean,
    override val operatingMode: NetworkInterfaceMode,
    val state: WirelessTetheringState,
    override val network: WifiNetwork? = null,
    override val caps: Set<WirelessNetworkInterfaceCapability> = emptySet(),
): SharedWifiInterfaceInstance(
    name = name,
    isUsable = isUsable,
    network = network,
    caps = caps,
    operatingMode = operatingMode,
) {
    override val mode: InterfaceMode = InterfaceMode.Tether
}
