package no.iktdev.kammich.system.network.wifi.scan

import no.iktdev.kammich.models.internal.network.WifiScanResult
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiScanState
import no.iktdev.kammich.system.network.wifi.WifiRunner
import no.iktdev.kammich.system.network.wifi.parser.NmcliHelper
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Order(2)
@Component
class NmcliScanStrategy(private val runner: WifiRunner) : WifiScanStrategy, NmcliHelper() {
    private val log = LoggerFactory.getLogger(NmcliScanStrategy::class.java)

    override fun scan(interfaceName: String): WifiScanResult {
        val result = runner.run("nmcli", "-t", "-f", "SSID,BSSID,SECURITY,SIGNAL,CHAN,FREQ", "device", "wifi", "list", "ifname", interfaceName)

        if (result !is WifiRunner.CommandResult.Success) {
            log.error("Nmcli scan feilet: ${result.let { (it as? WifiRunner.CommandResult.Failure)?.error }}")
            return WifiScanResult(
                WifiScanState.ERROR,
                false,
                networks = emptyList(),
            )
        }

        val networks = result.output.lines().filter { it.isNotBlank() }.mapNotNull { line ->
            try {
                val parts = line.split(Regex("(?<!\\\\):"))
                val ssid = parts[0]
                val bssid = parts[1].replace(
                    "\\",
                    ""
                ).let { if (it.length != 17) "00:00:00:00:00:00" else it }
                val security = parts[2]
                val signal = parts[3].toIntOrNull() ?: 0
                val channel = parts[4].toIntOrNull() ?: 0
                val freq = parts.getOrNull(5)?.replace(" MHz", "")?.toIntOrNull()

                WifiNetwork(
                    ssid = if (ssid.isBlank()) "<hidden>" else ssid,
                    signalPercent = signal,
                    isSecure = security.isNotBlank() && security != "--",
                    bssid = bssid,
                    securityType = security,
                    interfaceName = interfaceName,
                    isHidden = ssid.isBlank(),
                    channel = channel,
                    frequencyMhz = freq,
                    hwMode = if (freq != null && freq < 5000) "g" else "a"
                )
            } catch (e: Exception) {
                null
            }
        }

        return WifiScanResult(networks = networks, state =  WifiScanState.IDLE, success =  true)
    }

    override fun isSupported(): Boolean {
        // Vi sjekker om 'nmcli' eksisterer ved å prøve å kjøre kommandoen raskt
        // Dette er mer effektivt enn Runtime.exec hver gang
        val result = runner.run("which", "nmcli")
        return result is WifiRunner.CommandResult.Success
    }
}