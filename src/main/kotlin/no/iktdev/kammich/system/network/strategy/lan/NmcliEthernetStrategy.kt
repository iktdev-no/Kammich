package no.iktdev.kammich.system.network.strategy.lan

import no.iktdev.kammich.models.internal.network.NmCliDeviceState
import no.iktdev.kammich.models.shared.network.InterfaceMode
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode
import no.iktdev.kammich.system.SysCommand
import no.iktdev.kammich.system.network.al.INmcliAL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NmcliEthernetStrategy(
    private val exec: SysCommand,
    private val nmcliAL: INmcliAL
) : EthernetModeStrategy {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun startClientMode(interfaceName: String): Boolean {
        val connectionName = "Kammich-Ethernet-Client-$interfaceName"

        nmcliAL.deleteConnection(connectionName)

        val created = nmcliAL.createEthernetClientConnection(
            ifName = interfaceName,
            connectionName = connectionName,
            autoConnect = false
        )

        if (!created.isSuccess()) {
            throw IllegalArgumentException(
                "Cannot create Ethernet client connection $connectionName"
            )
        }

        val connected = nmcliAL.connect(connectionName)

        if (!connected.isSuccess()) {
            nmcliAL.deleteConnection(connectionName)
            throw RuntimeException(
                "Unable to connect Ethernet client $interfaceName"
            )
        }

        log.info("Ethernet $interfaceName started in client mode")
        return true
    }

    override fun stopClientMode(interfaceName: String) {
        val connectionName = "Kammich-Ethernet-Client-$interfaceName"

        nmcliAL.disconnect(interfaceName)
        nmcliAL.deleteConnection(connectionName)

        log.info("Ethernet $interfaceName stopped client mode")
    }

    override fun startTetherMode(interfaceName: String): Boolean {
        val connectionName = "Kammich-Ethernet-Tether-$interfaceName"

        nmcliAL.deleteConnection(connectionName)

        val created = nmcliAL.createEthernetHostConnection(
            ifName = interfaceName,
            connectionName = connectionName,
            autoConnect = false
        )

        if (!created.isSuccess()) {
            throw IllegalArgumentException(
                "Cannot create Ethernet tether connection $connectionName"
            )
        }

        val connected = nmcliAL.connect(connectionName)

        if (!connected.isSuccess()) {
            nmcliAL.deleteConnection(connectionName)
            throw RuntimeException(
                "Unable to start Ethernet tether mode on $interfaceName"
            )
        }

        log.info("Ethernet $interfaceName started in tether mode")
        return true
    }

    override fun stopTetherMode(interfaceName: String) {
        val connectionName = "Kammich-Ethernet-Tether-$interfaceName"

        nmcliAL.disconnect(interfaceName)
        nmcliAL.deleteConnection(connectionName)

        log.info("Ethernet $interfaceName stopped tether mode")
    }

    override fun isSupported(): Boolean {
        return exec.nonSudo("which", "nmcli").isSuccess()
    }

    override fun getState(interfaceName: String): NetworkInterfaceMode {
        val connectionName = nmcliAL.getConnectionName(interfaceName)
            ?: return NetworkInterfaceMode.Idle

        return when (nmcliAL.getEthernetMode(connectionName)) {
            InterfaceMode.Client -> NetworkInterfaceMode.Client
            InterfaceMode.Tether -> NetworkInterfaceMode.Tether
            else -> NetworkInterfaceMode.Idle
        }
    }

    override fun getProfileIpv4Address(interfaceName: String): String? {
        val connectionName = nmcliAL.getConnectionName(interfaceName)
            ?: return null

        return nmcliAL.getConnectionIpv4Address(connectionName)
    }

    override fun getDeviceIpv4Addresses(interfaceName: String): String? {
        return nmcliAL.getDeviceIpv4Address(interfaceName)
    }

    override fun hasCarrier(interfaceName: String): Boolean =
        nmcliAL.hasEthernetCarrier(interfaceName)

    override fun getDeviceState(interfaceName: String): NmCliDeviceState =
        nmcliAL.getDeviceState(interfaceName)

}