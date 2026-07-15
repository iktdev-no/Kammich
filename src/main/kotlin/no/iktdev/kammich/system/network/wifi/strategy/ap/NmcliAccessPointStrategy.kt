package no.iktdev.kammich.system.network.wifi.strategy.ap

import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiSecurityType
import no.iktdev.kammich.models.shared.network.WifiTetherSetting
import no.iktdev.kammich.models.shared.network.WifiTetheringNetwork
import no.iktdev.kammich.models.shared.network.WifiTetheringState
import no.iktdev.kammich.system.network.wifi.WifiRunner
import no.iktdev.kammich.system.network.wifi.parser.NmcliHelper
import no.iktdev.kammich.system.network.wifi.parser.WifiPhyInfoParser
import org.springframework.stereotype.Component
import java.io.File
import org.slf4j.LoggerFactory

@Component
class NmcliAccessPointStrategy(
    private val runner: WifiRunner,
    private val hostpadAligned: WifiHostpadAligned,
) : AccessPointStrategy, NmcliHelper() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun start(interfaceName: String, tether: WifiTetherSetting): WifiRunner.CommandResult {
        return runner.run(
            "sudo","nmcli", "device", "wifi", "hotspot",
            "ifname", interfaceName,
            "ssid", tether.ssid,
            "password", tether.password,
        )
    }

    override fun startAligned(interfaceName: String, tether: WifiTetherSetting, network: WifiNetwork): WifiTetheringNetwork? {
        logger.info("Aligning AP to SSID: ${network.ssid} on channel ${network.channel}")
        // 1. Force stop alt annet (Fail-safe: rydder vei før vi tar over)
        stop(interfaceName)

        val result = runner.run("sudo", "nmcli", "device", "set", interfaceName, "managed", "no")
        if (result is WifiRunner.CommandResult.Failure) {
            logger.error("Failed to releasing the interface $interfaceName, thus making aligned tether impossible", result.error)
            return null
        }

        return hostpadAligned.startAligned(interfaceName, tether, network)
    }

    override fun stop(interfaceName: String): Boolean {
        if (hostpadAligned.isRunning()) {
            return hostpadAligned.stopAligned()
        }

        val stopped = runner.run("sudo", "nmcli", "connection", "down", "Hotspot")
        return when (stopped) {
            is WifiRunner.CommandResult.Success -> {
                logger.info("Stopped AP")
                true
            }
            is WifiRunner.CommandResult.Failure -> {
                logger.error("Failed to stop AP", stopped.error)
                false
            }
        }
    }

    override fun getTetheringStatus(interfaceName: String): WifiTetheringState {
        // 1. Sjekk hostapd først (koster ingenting)
        if (hostpadAligned.isRunning()) {
            return WifiTetheringState.RUNNING_ALIGNED
        }

        // 2. Sjekk NM status
        val nmResult = runner.run("sudo", "nmcli", "-t", "-f", "GENERAL.STATE,GENERAL.CONNECTION", "device", "show", interfaceName)

        // Vi bruker 'when' på resultatet for å unngå usikre 'map'-kall
        return when (nmResult) {
            is WifiRunner.CommandResult.Success -> {
                val output = nmResult.output
                val isConnected = output.contains("connected")
                val hasHotspot = output.contains("Hotspot")
                val isDisconnected = output.contains("disconnected")

                when {
                    isConnected && hasHotspot -> WifiTetheringState.RUNNING
                    isConnected -> WifiTetheringState.ERROR // Tilkoblet noe annet (f.eks. vanlig WiFi)
                    isDisconnected -> WifiTetheringState.IDLE
                    else -> WifiTetheringState.IDLE
                }
            }
            is WifiRunner.CommandResult.Failure -> {
                // Hvis kommandoen feiler (f.eks. interface finnes ikke), er vi IDLE eller feil
                WifiTetheringState.IDLE
            }
        }
    }

    override fun getActiveTethering(interfaceName: String): WifiTetheringNetwork? {
        // 1. Sjekk først om vi kjører i "Aligned" modus (Hostapd)
        val hostapdActive = hostpadAligned.getActiveTethering()
        if (hostapdActive != null) {
            return hostapdActive
        }

        // 2. Hvis hostapd ikke kjører, sjekk om NetworkManager har en aktiv hotspot på dette interfacet
        val nmResult = runner.run("nmcli", "-t", "-f", "GENERAL.CONNECTION,GENERAL.DEVICE", "device", "show", interfaceName)

        if (nmResult is WifiRunner.CommandResult.Success) {

            val (channel, freq) = runner.run("sudo", "iw", "dev", interfaceName, "info")
                .map { WifiPhyInfoParser().getWifiInfoFromInterface(it) } ?: (0 to 0)


            // Vi parser outputen for å se om det finnes en aktiv connection
            val output = nmResult.output

            // Sjekker om det finnes en linje som indikerer en aktiv tilkobling (ikke tom)
            // Her antar vi at du ser etter profiler som starter med "kammich-hotspot-"
            val connectionLine = output.lines().find { it.startsWith("GENERAL.CONNECTION:") }
            val connectionName = connectionLine?.substringAfter(":")?.takeIf { it.isNotBlank() && it != "--" }

            if (connectionName != null) {
                // Vi henter SSID fra profilen via nmcli
                val ssidResult = runner.run("nmcli", "-s", "-g", "802-11-wireless.ssid", "connection", "show", connectionName)
                val ssid = if (ssidResult is WifiRunner.CommandResult.Success) ssidResult.output.trim() else "Unknown"

                return WifiTetheringNetwork(
                    ssid = ssid,
                    channel = channel, // NM styrer kanal automatisk, vi vet ikke alltid hvilken
                    frequencyMhz = freq,
                    isAligned = false,
                    alignedToSSID = null
                )
            }
        }

        return null
    }
}