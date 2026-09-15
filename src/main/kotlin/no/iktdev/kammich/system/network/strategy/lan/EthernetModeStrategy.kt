package no.iktdev.kammich.system.network.strategy.lan

import no.iktdev.kammich.models.internal.network.NmCliDeviceState
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode

interface EthernetModeStrategy {
    fun startClientMode(interfaceName: String): Boolean
    fun stopClientMode(interfaceName: String)
    fun startTetherMode(interfaceName: String): Boolean
    fun stopTetherMode(interfaceName: String)
    fun isSupported(): Boolean
    fun getState(interfaceName: String): NetworkInterfaceMode
    fun getProfileIpv4Address(interfaceName: String): String?
    fun hasCarrier(interfaceName: String): Boolean
    fun getDeviceState(interfaceName: String): NmCliDeviceState
    fun getDeviceIpv4Addresses(interfaceName: String): String?
}