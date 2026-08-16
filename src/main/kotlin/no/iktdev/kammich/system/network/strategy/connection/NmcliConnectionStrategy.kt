package no.iktdev.kammich.system.network.strategy.connection

import no.iktdev.kammich.models.shared.network.*
import no.iktdev.kammich.system.SysCommand
import no.iktdev.kammich.system.network.al.INmcliAL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NmcliConnectionStrategy(
    private val exec: SysCommand,
    private val nmcliAL: INmcliAL
) : WifiConnectionStrategy {
    private val log = LoggerFactory.getLogger(NmcliConnectionStrategy::class.java)

    fun setAutoconnect(profileName: String, enable: Boolean): Boolean {
        val result = nmcliAL.setAutoConnect(profileName, enable)

        if (result) {
            log.info("Autoconnect set to $enable for $profileName")
        } else {
            log.error("Failed to set autoconnect for $profileName: ${result}")
        }
        return result
    }

    override fun connect(interfaceName: String, network: WifiNetwork, password: String?): WifiConnection {
        val connectionName = if (!network.isHidden) network.ssid else "Hidden${network.bssid}"

        nmcliAL.deleteConnection(network.ssid)
        val created = nmcliAL.createWifiClientConnection(
            interfaceName,
            connectionName,
            network.ssid,
            network.bssid,
            password = password,
            securityType = network.securityType
        )

        if (!created.isSuccess()) {
            throw IllegalArgumentException("Cannot create connection profile to ${network.ssid}/$connectionName")
        }

        val connectResult = nmcliAL.connect(connectionName)

        return connectResult.fold(
            onSuccess = {
                WifiConnection(interfaceName, WifiConnectionStateType.Connected, network, null)
            },
            onFailure = { out, err, code ->
                if (code == 4) {
                    log.info("Exit code 4 received (Wrong password).")
                    WifiConnection(interfaceName, WifiConnectionStateType.Disconnected, network, WifiInterfaceClientError.WrongPassword)
                } else {
                    log.error("Failed to connect to ${network.ssid} on $connectionName. Exit Code $code, error: $err")
                    nmcliAL.deleteConnection(connectionName)
                    throw RuntimeException("Unable to connect to network $network: $err")
                }
            }
        ) ?: throw RuntimeException("Ukjent feil ved tilkobling til $network")
    }

    override fun disconnect(interfaceName: String): WifiConnection {
        val connection = nmcliAL.getConnectionName(interfaceName)
        if (!connection.isNullOrBlank()) {
            nmcliAL.dropConnection(connection)
        }
        val disconnect = nmcliAL.disconnect(interfaceName)

        return if (disconnect.isSuccess()) {
            WifiConnection(interfaceName, WifiConnectionStateType.Disconnected, null)
        } else {
            WifiConnection(interfaceName, WifiConnectionStateType.Idle, null)
        }
    }

    override fun isSupported(): Boolean {
        return exec.nonSudo("which", "nmcli").isSuccess()
    }

    override fun getState(interfaceName: String): WifiConnection {
        val connectionName = nmcliAL.getConnectionName(interfaceName)
            ?: return WifiConnection(ifName = interfaceName, state = WifiConnectionStateType.Disconnected, null)

        val mode = nmcliAL.getWirelessMode(connectionName)
        // Tilstand basert på om modusen er Client (eller infrastruktur)
        val state = if (mode == InterfaceMode.Client) WifiConnectionStateType.Connected else WifiConnectionStateType.Disconnected
        val network = getNetwork(interfaceName)
        return WifiConnection(
            ifName = interfaceName,
            state = state,
            network = network,
        )
    }

    override fun getNetwork(interfaceName: String): WifiNetwork? {
        val connectionName = nmcliAL.getConnectionName(interfaceName) ?: return null
        val mode = nmcliAL.getWirelessMode(connectionName)

        if (mode != InterfaceMode.Client) {
            return null
        }

        val networks = nmcliAL.getNetworks(interfaceName)
        return networks.find { it.interfaceName == interfaceName && (it.isActive || it.inUse) }
    }
}