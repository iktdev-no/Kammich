package no.iktdev.kammich.system.network.wifi.strategy.ap

import no.iktdev.kammich.models.shared.network.InterfaceActiveState
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiNetworkHardwareMode
import no.iktdev.kammich.models.shared.network.WifiNetworkTether
import no.iktdev.kammich.models.shared.network.WifiSecurityType
import no.iktdev.kammich.models.shared.network.WifiTetherAP
import no.iktdev.kammich.models.shared.network.WirelessTetheringState
import no.iktdev.kammich.system.SysCommand
import no.iktdev.kammich.system.network.components.IW
import no.iktdev.kammich.system.network.components.Nmcli
import no.iktdev.kammich.system.network.wifi.WifiTetherService
import no.iktdev.kammich.system.network.wifi.parser.NmcliHelper
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import kotlin.math.log

@Component
class NmcliAccessPointStrategy(
    private val exec: SysCommand,
    private val iw: IW,
    private val nmcli: Nmcli,
) : AccessPointStrategy, NmcliHelper() {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun cleanup() {
        val cleaned = exec.sudo("nmcli", "con", "delete", WifiTetherService.ap_profileName)
    }

    fun setAutoconnect(profileName: String, enable: Boolean): Boolean {
        val value = if (enable) "yes" else "no"
        val result = exec.sudo(
            "nmcli", "con", "modify", profileName,
            "connection.autoconnect", value
        )

        if (result.isSuccess()) {
            logger.info("Autoconnect set to $value for $profileName")
            return true
        } else {
            logger.error("Failed to set autoconnect for $profileName: ${result}")
            return false
        }
    }

    override fun start(interfaceName: String, tether: WifiTetherAP, autoconnect: Boolean): SysCommand.Result {

        cleanup()

        val sec = when (tether.security) {
            WifiSecurityType.WPA2 -> "wpa-psk"
            WifiSecurityType.WPA3 -> "sae"
            else -> null
        }
        val basisCommand = listOf(
            "nmcli", "con", "add",
            "type", "wifi",
            "ifname", interfaceName,
            "con-name", WifiTetherService.ap_profileName,
            "ssid", tether.ssid,
            "802-11-wireless.mode", "ap", // Dette er den moderne måten
            "802-11-wireless.band", "bg", // 2.4GHz (legg til "a" for 5GHz hvis støttet)
            "ipv4.method", "shared",
            "connection.autoconnect", if (autoconnect) "yes" else "no",
        )
        val password = if (sec != null) {
            listOf(
                "wifi-sec.key-mgmt", sec,
                "wifi-sec.psk", tether.password
            )
        } else emptyList()
        logger.info("Creating NMCLI access point profile")
        val success = exec.sudo(
            basisCommand + password,
        ).isSuccess()
        if (!success) {
            logger.error("Unable to create NMCLI access point profile")
            return SysCommand.Result.Failure("Something went wrong")
        }
        logger.info("Starting NMCLI access point strategy for $interfaceName with command ${(basisCommand + password).joinToString(" ")}")
        return exec.sudo(
            "nmcli", "con", "up", WifiTetherService.ap_profileName
        )
    }

    override fun stop(interfaceName: String): Boolean {
        val command = listOf("nmcli", "connection", "down", WifiTetherService.ap_profileName)
        val stopped = exec.sudo(*command.toTypedArray())
        if (!stopped.isSuccess()) {
            return false
        }
        logger.info("Stopped AP")
        cleanup()
        return stopped.isSuccess()
    }

    override fun getState(interfaceName: String): WifiNetworkTether {
        // 0 Sjekk om vi er i AP føst
        val mode = iw.getMode(interfaceName)
        if (mode != IW.InterfaceMode.AP) {
            logger.debug("Not in ap mode...")
            return WifiNetworkTether(interfaceName, WirelessTetheringState.Idle, null)
        }

        val general = nmcli.general(interfaceName)
        if (general?.state != Nmcli.NmState.CONNECTED) {
            return WifiNetworkTether(interfaceName, WirelessTetheringState.Idle, null)
        }

        val wifi = nmcli.wifi().filter { it.device == interfaceName }.find { it.inUse }  ?: return WifiNetworkTether(interfaceName, WirelessTetheringState.Idle)

        val freq = wifi.frequency.split(" ")[0].toIntOrNull() ?: -1

        return WifiNetworkTether(
            name = interfaceName,
            state = WirelessTetheringState.Broadcasting,
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