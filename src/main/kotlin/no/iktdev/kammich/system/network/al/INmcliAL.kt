package no.iktdev.kammich.system.network.al

import no.iktdev.kammich.models.internal.network.NmCliDevice
import no.iktdev.kammich.models.internal.network.NmCliDeviceState
import no.iktdev.kammich.models.shared.network.InterfaceMode
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.system.SysCommand

interface INmcliAL {

    fun getDevices(): List<NmCliDevice>

    fun getDeviceHWADDR(ifName: String): String?

    fun getConnectionName(ifName: String): String?

    fun getWirelessMode(connectionName: String): InterfaceMode?

    fun deleteConnection(connectionName: String): Boolean
    fun setAutoConnect(connectionName: String, autoConnect: Boolean): Boolean
    fun createWifiClientConnection(
        ifName: String,
        connectionName: String,
        ssid: String,
        bssid: String,
        password: String?,
        securityType: String?,
        autoConnect: Boolean = false
    ): SysCommand.Result

    fun connect(connectionName: String): SysCommand.Result
    fun dropConnection(connectionName: String): Boolean
    fun disconnect(interfaceName: String): SysCommand.Result
    fun scan(ifName: String): SysCommand.Result
    fun getNetworks(interfaceName: String): List<WifiNetwork>
    fun createWifiTetherConnection(
        ifName: String,
        connectionName: String,
        ssid: String,
        password: String?,
        securityType: String?,
        autoConnect: Boolean
    ): SysCommand.Result

    fun createEthernetHostConnection(
        ifName: String,
        connectionName: String,
        ipAddress: String = "10.0.0.1",
        prefix: Int = 24,
        autoConnect: Boolean = true
    ): SysCommand.Result

    fun getConnectionIpv4Address(connectionName: String): String?
    fun getEthernetMode(connectionName: String): InterfaceMode?
    fun isDeviceManaged(ifName: String): Boolean
    fun createEthernetClientConnection(ifName: String, connectionName: String, autoConnect: Boolean): SysCommand.Result
    fun hasEthernetCarrier(ifName: String): Boolean
    fun getDeviceState(ifName: String): NmCliDeviceState
    fun getDeviceIpv4Address(interfaceName: String): String?
}