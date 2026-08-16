package no.iktdev.kammich.system.network.al

import no.iktdev.kammich.models.shared.network.WirelessNetworkInterfaceCapability

interface IIwAL {
    fun getWirelessCapabilities(phy: String): Set<WirelessNetworkInterfaceCapability>
    fun getPhysicalInterfaces(ifName: String): String?
}