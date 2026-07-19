package no.iktdev.kammich.system.network.wifi.strategy.connection

import no.iktdev.kammich.models.shared.network.InterfaceActiveState
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiNetworkConnection
import no.iktdev.kammich.models.shared.network.WifiNetworkHardwareMode
import no.iktdev.kammich.system.SysCommand
import no.iktdev.kammich.system.network.components.IW
import no.iktdev.kammich.system.network.components.Nmcli
import no.iktdev.kammich.system.network.wifi.WifiRunner
import no.iktdev.kammich.system.network.wifi.parser.NmcliHelper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NmcliConnectionStrategy(
    private val exec: SysCommand,
    private val iw: IW,
    private val nmcli: Nmcli,
    private val runner: WifiRunner
) : WifiConnectionStrategy, NmcliHelper() {
    private val log = LoggerFactory.getLogger(NmcliConnectionStrategy::class.java)


    override fun connect(interfaceName: String, network: WifiNetwork, password: String?): WifiNetworkConnection {
        val profileName = "Kammich-${network.bssid}"
        runner.run("sudo", "nmcli", "connection", "delete", profileName)

        // Vi må spesifisere 802-11-wireless.bssid for at nmcli skal forstå det
        val command = mutableListOf(
            "sudo", "nmcli", "con", "add",
            "type", "wifi",
            "ifname", interfaceName,
            "con-name", profileName,
            "ssid", network.ssid,
            "802-11-wireless.bssid", network.bssid
        )

        if (!password.isNullOrBlank()) {
            val keyMgmt = when {
                network.securityType.contains("WPA3", ignoreCase = true) -> "sae"
                network.securityType.contains("WPA2", ignoreCase = true) ||
                        network.securityType.contains("WPA", ignoreCase = true) -> "wpa-psk"
                else -> "wpa-psk"
            }
            // Sikkerhetsinnstillinger må også ha setting-prefikset
            command.addAll(listOf(
                "wifi-sec.key-mgmt", keyMgmt,
                "wifi-sec.psk", password
            ))
        } else {
            command.addAll(listOf("wifi-sec.key-mgmt", "none"))
        }

        val addResult = runner.run(*command.toTypedArray())
        if (addResult is WifiRunner.CommandResult.Failure) {
            throw RuntimeException(addResult.error)
        }

        val upResult = runner.run("sudo", "nmcli", "con", "up", profileName)
        return if (upResult is WifiRunner.CommandResult.Success) {
            WifiNetworkConnection(interfaceName, InterfaceActiveState.Connected, network)
        } else {
            throw RuntimeException("Unable to connect to network $network")
        }
    }

    override fun disconnect(interfaceName: String): WifiNetworkConnection {
        val result = exec.sudo("nmcli", "device", "disconnect", interfaceName)
        return if (result.isSuccess()) {
            WifiNetworkConnection(interfaceName, InterfaceActiveState.Disconnected)
        } else {
            WifiNetworkConnection(interfaceName, InterfaceActiveState.Idle)
        }
    }

    override fun getState(interfaceName: String): WifiNetworkConnection {
        // 0 Sjekk om vi er i AP føst
        val usabale = iw.getMode(interfaceName)
        if (usabale == IW.InterfaceMode.AP) {
            return WifiNetworkConnection(interfaceName, InterfaceActiveState.Disconnected, null)
        }

        val general = nmcli.general(interfaceName)
        if (general?.state != Nmcli.NmState.CONNECTED) {
            return WifiNetworkConnection(interfaceName, InterfaceActiveState.Disconnected, null)
        }

        val wifi = nmcli.wifi().find { it.device == interfaceName } ?: return WifiNetworkConnection(interfaceName, InterfaceActiveState.Disconnected)

        val freq = wifi.frequency.split(" ")[0].toIntOrNull() ?: -1

        return WifiNetworkConnection(
            name = interfaceName,
            state = InterfaceActiveState.Connected,
            network = WifiNetwork(
                ssid = wifi.ssid ?: "",
                signalPercent = wifi.signal,
                isSecure = wifi.security.isNotBlank(),
                bssid = wifi.bssid ?: "00:00:00:00:00:00",
                securityType = wifi.security,
                interfaceName = interfaceName,
                isHidden = wifi.ssid.isNullOrBlank(),
                channel = wifi.channel,
                frequencyMhz = freq,
                hwMode = if (freq < 5000) WifiNetworkHardwareMode.g else WifiNetworkHardwareMode.a
            )
        )
    }

}