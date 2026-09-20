package no.iktdev.kammich.system.network.strategy.connection

import no.iktdev.kammich.models.internal.network.NmCliDeviceState
import no.iktdev.kammich.models.shared.network.*
import no.iktdev.kammich.system.SysCommand
import no.iktdev.kammich.system.network.al.INmcliAL
import no.iktdev.kammich.system.network.strategy.state.NmcliWifiStateStrategy
import no.iktdev.kammich.system.network.strategy.state.WifiStateStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NmcliWifiConnectionStrategy(
    private val strategy: WifiStateStrategy,
    private val exec: SysCommand,
    private val nmcliAL: INmcliAL
) : WifiConnectionStrategy {
    private val log = LoggerFactory.getLogger(NmcliWifiConnectionStrategy::class.java)

    fun setAutoconnect(profileName: String, enable: Boolean): Boolean {
        val result = nmcliAL.setAutoConnect(profileName, enable)

        if (result) {
            log.info("Autoconnect set to $enable for $profileName")
        } else {
            log.error("Failed to set autoconnect for $profileName: ${result}")
        }
        return result
    }

    override fun connect(
        interfaceName: String,
        network: WifiNetwork,
        password: String?
    ): WifiInterfaceState {
        val connectionName =
            if (!network.isHidden) network.ssid else "Hidden${network.bssid}"

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
            throw IllegalArgumentException(
                "Cannot create connection profile to ${network.ssid}/$connectionName"
            )
        }

        val connectResult = nmcliAL.connect(connectionName)

        return connectResult.fold(
            onSuccess = {
                WifiInterfaceState(
                    ifName = interfaceName,
                    state = WifiInterfaceStateType.Connected,
                    operatingMode = NetworkInterfaceMode.Client,
                    network = network,
                    ipv4 = nmcliAL.getDeviceIpv4Address(interfaceName)
                )
            },
            onFailure = { _, err, code ->
                if (code == 4) {
                    log.info("Exit code 4 received (Wrong password).")

                    WifiInterfaceState(
                        ifName = interfaceName,
                        state = WifiInterfaceStateType.Disconnected,
                        operatingMode = NetworkInterfaceMode.Client,
                        network = network,
                        error = WifiInterfaceErrorType.ClientWrongPassword
                    )
                } else {
                    log.error(
                        "Failed to connect to ${network.ssid} on $connectionName. " +
                                "Exit Code $code, error: $err"
                    )

                    nmcliAL.deleteConnection(connectionName)

                    throw RuntimeException(
                        "Unable to connect to network $network: $err"
                    )
                }
            }
        ) ?: throw RuntimeException(
            "Ukjent feil ved tilkobling til $network"
        )
    }

    override fun disconnect(interfaceName: String): WifiInterfaceState {
        val connection = nmcliAL.getConnectionName(interfaceName)

        if (!connection.isNullOrBlank()) {
            nmcliAL.dropConnection(connection)
        }

        val disconnect = nmcliAL.disconnect(interfaceName)

        return WifiInterfaceState(
            ifName = interfaceName,
            state = if (disconnect.isSuccess()) {
                WifiInterfaceStateType.Disconnected
            } else {
                WifiInterfaceStateType.Idle
            },
            operatingMode = NetworkInterfaceMode.Client
        )
    }

    override fun getState(ifName: String): WifiInterfaceState =
        strategy.getState(ifName)

    override fun getNetwork(interfaceName: String): WifiNetwork? =
        strategy.getNetwork(interfaceName)

    override fun isSupported(): Boolean {
        return exec.nonSudo("which", "nmcli").isSuccess()
    }

}