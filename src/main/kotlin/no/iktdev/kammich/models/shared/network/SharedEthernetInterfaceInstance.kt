package no.iktdev.kammich.models.shared.network

abstract class SharedEthernetInterfaceInstance (
    open val name: String,
    open val isUsable: Boolean,
    open val operatingMode: NetworkInterfaceMode
) {
    abstract val mode: InterfaceMode
}

data class EthernetInterfaceClient(
    override val name: String,
    override val isUsable: Boolean,
    override val operatingMode: NetworkInterfaceMode
): SharedEthernetInterfaceInstance(name, isUsable, operatingMode) {
    override val mode: InterfaceMode = InterfaceMode.Client
}

data class EthernetInterfaceTether(
    override val name: String,
    override val isUsable: Boolean,
    override val operatingMode: NetworkInterfaceMode
): SharedEthernetInterfaceInstance(name, isUsable, operatingMode) {
    override val mode: InterfaceMode = InterfaceMode.Tether
}
