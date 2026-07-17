package no.iktdev.kammich.system.network.wifi.strategy.ap

import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiSecurityType
import no.iktdev.kammich.models.shared.network.WifiTetherSetting
import no.iktdev.kammich.models.shared.network.WifiTetheringNetwork
import no.iktdev.kammich.models.shared.network.WifiTetheringState
import no.iktdev.kammich.system.network.wifi.WifiRunner
import no.iktdev.kammich.system.network.wifi.WifiTetherService
import no.iktdev.kammich.system.network.wifi.parser.NmcliHelper
import no.iktdev.kammich.system.network.wifi.parser.WifiPhyInfoParser
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory

@Component
class NmcliAccessPointStrategy(
    private val runner: WifiRunner,
) : AccessPointStrategy, NmcliHelper() {

    private val logger = LoggerFactory.getLogger(javaClass)


    override fun start(interfaceName: String, tether: WifiTetherSetting): WifiRunner.CommandResult {
        val sec = when (tether.security) {
            WifiSecurityType.WPA2 -> "wpa-psk"
            WifiSecurityType.WPA3 -> "sae"
            else -> null
        }
        val basisCommand = listOf(
            "sudo", "nmcli", "con", "add",
            "type", "wifi",
            "ifname", interfaceName,
            "con-name", WifiTetherService.ap_profileName,
            "ssid", tether.ssid,
            "802-11-wireless.mode", "ap", // Dette er den moderne måten
            "802-11-wireless.band", "bg", // 2.4GHz (legg til "a" for 5GHz hvis støttet)
            "ipv4.method", "shared",
        )
        val password = if (sec != null) {
            listOf(
                "wifi-sec.key-mgmt", sec,
                "wifi-sec.psk", tether.password
            )
        } else emptyList()
        return runner.run(
            basisCommand + password,
        )
    }

    override fun stop(interfaceName: String): Boolean {
        val command = listOf("sudo", "nmcli", "connection", "down", WifiTetherService.ap_profileName)

        val stopped = runner.run(*command.toTypedArray())
        return when (stopped) {
            is WifiRunner.CommandResult.Success -> {
                logger.info("Stopped AP")
                true
            }
            is WifiRunner.CommandResult.Failure -> {
                logger.error("Failed to stop AP using command: ${command.joinToString(" ")}", stopped.error)
                false
            }
        }
    }

    override fun getTetheringStatus(interfaceName: String): WifiTetheringState {
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

    override fun isManaged(interfaceName: String): Boolean {
        // Endret fra GENERAL.MANAGED til GENERAL.NM-MANAGED
        val result = runner.run("nmcli", "-t", "-f", "GENERAL.NM-MANAGED", "device", "show", interfaceName)

        return when (result) {
            is WifiRunner.CommandResult.Success -> {
                // NM returnerer "yes" eller "no"
                val output = result.output.trim()
                val managed = output.split(":")[1].equals("yes", ignoreCase = true)
                logger.debug("Interface $interfaceName er managed: $managed,\n$output")
                managed
            }
            else -> false
        }
    }

    override fun unmanage(interfaceName: String): Boolean {
        logger.info("Deaktiverer NetworkManager for interface: $interfaceName")
        val result = runner.run("sudo", "nmcli", "device", "set", interfaceName, "managed", "no")

        return when (result) {
            is WifiRunner.CommandResult.Success -> {
                logger.info("Interface $interfaceName er nå unmanaged")
                true
            }
            is WifiRunner.CommandResult.Failure -> {
                logger.error("Klarte ikke å sette $interfaceName til unmanaged: ${result.error}")
                false
            }
        }
    }

    override fun manage(interfaceName: String): Boolean {
        logger.info("Aktiverer NetworkManager for interface: $interfaceName")
        val result = runner.run("sudo", "nmcli", "device", "set", interfaceName, "managed", "yes")

        return when (result) {
            is WifiRunner.CommandResult.Success -> {
                logger.info("Interface $interfaceName er nå managed igjen")
                true
            }
            is WifiRunner.CommandResult.Failure -> {
                logger.error("Klarte ikke å sette $interfaceName til managed: ${result.error}")
                false
            }
        }
    }

    override fun getActiveTethering(interfaceName: String): WifiTetheringNetwork? {
        // 1. Sjekk først om vi kjører i "Aligned" modus (Hostapd)
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
                )
            }
        }

        return null
    }
}