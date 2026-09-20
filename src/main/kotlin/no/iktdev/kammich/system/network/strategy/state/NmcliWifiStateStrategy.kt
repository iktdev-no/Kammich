package no.iktdev.kammich.system.network.strategy.state

import no.iktdev.kammich.models.internal.network.NmCliDeviceState
import no.iktdev.kammich.models.shared.network.InterfaceMode
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode
import no.iktdev.kammich.models.shared.network.WifiInterfaceState
import no.iktdev.kammich.models.shared.network.WifiInterfaceStateType
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.system.SysCommand
import no.iktdev.kammich.system.network.al.INmcliAL
import org.springframework.stereotype.Component

@Component
class NmcliWifiStateStrategy(
    private val nmcliAL: INmcliAL,
    private val exec: SysCommand,
): WifiStateStrategy {

    override fun getState(interfaceName: String): WifiInterfaceState {
        val connectionName = nmcliAL.getConnectionName(interfaceName)
            ?: return idleState(interfaceName)

        val mode = nmcliAL.getWirelessMode(connectionName)
            ?: return idleState(interfaceName)

        val operatingMode = mode.toNetworkInterfaceMode()
        val deviceState = nmcliAL.getDeviceState(interfaceName)

        return WifiInterfaceState(
            ifName = interfaceName,
            state = getState(operatingMode, deviceState),
            operatingMode = operatingMode,
            deviceState = deviceState,
            network = getNetwork(interfaceName),
            ipv4 = nmcliAL.getDeviceIpv4Address(interfaceName)
        )
    }

    private fun getState(
        operatingMode: NetworkInterfaceMode,
        deviceState: NmCliDeviceState
    ): WifiInterfaceStateType =
        when (operatingMode) {
            NetworkInterfaceMode.Client -> when (deviceState) {
                NmCliDeviceState.Activated ->
                    WifiInterfaceStateType.Connected

                NmCliDeviceState.Prepare,
                NmCliDeviceState.Config,
                NmCliDeviceState.NeedAuth,
                NmCliDeviceState.IpConfig,
                NmCliDeviceState.IpCheck ->
                    WifiInterfaceStateType.Connecting

                else ->
                    WifiInterfaceStateType.Disconnected
            }

            NetworkInterfaceMode.Tether ->
                WifiInterfaceStateType.Tethering

            else ->
                WifiInterfaceStateType.Idle
        }

    override fun getNetwork(interfaceName: String): WifiNetwork? {
        val networks = nmcliAL.getNetworks(interfaceName)

        return networks.find {
            it.interfaceName == interfaceName &&
                    (it.isActive || it.inUse)
        }
    }

    private fun idleState(interfaceName: String) =
        WifiInterfaceState(
            ifName = interfaceName,
            state = WifiInterfaceStateType.Idle,
            operatingMode = NetworkInterfaceMode.Idle
        )

    private fun InterfaceMode.toNetworkInterfaceMode(): NetworkInterfaceMode =
        when (this) {
            InterfaceMode.Client -> NetworkInterfaceMode.Client
            InterfaceMode.Tether -> NetworkInterfaceMode.Tether
            else -> NetworkInterfaceMode.Idle
        }

    override fun isSupported(): Boolean {
        return exec.nonSudo("which", "nmcli").isSuccess()
    }
}