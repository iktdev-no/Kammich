package no.iktdev.kammich.system.network.wifi.scan

import no.iktdev.kammich.models.internal.network.WifiScanResult
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiScanState
import no.iktdev.kammich.system.network.wifi.WifiRunner
import no.iktdev.kammich.utils.WifiUtils

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class IwctlScanStrategy(private val runner: WifiRunner) : WifiScanStrategy {
    private val log = LoggerFactory.getLogger(IwctlScanStrategy::class.java)
    private val mapper = jacksonObjectMapper()

    override fun scan(interfaceName: String): WifiScanResult {
        runner.run("iwctl", "station", interfaceName, "scan")
        val result = runner.run("iwctl", "station", interfaceName, "get-networks", "--json")

        if (result !is WifiRunner.CommandResult.Success) {
            log.error("Iwctl scan feilet: ${result.let { (it as? WifiRunner.CommandResult.Failure)?.error }}")
            return WifiScanResult(WifiScanState.ERROR, false, networks = emptyList())
        }

        return try {
            // IWD returnerer en liste av objekter
            val networks: List<Map<String, Any>> = mapper.readValue(result.output)

            val res = networks.map { net ->
                val ssid = net["Name"] as? String ?: ""
                val signal = (net["RSSI"] as? Number)?.toInt() ?: 0
                val security = net["Security"] as? String ?: "open"
                val freqMhz = (net["Frequency"] as? Number)?.toInt() // IWD gir ofte freq i MHz

                val isHidden = ssid.isBlank()

                WifiNetwork(
                    ssid = if (isHidden) "<hidden>" else ssid,
                    isHidden = isHidden,
                    signalPercent = WifiUtils.dBmToPercentage(signal),
                    isSecure = security.lowercase() != "open",
                    bssid = net["BSSID"] as? String ?: "00:00:00:00:00:00",
                    securityType = security,
                    interfaceName = interfaceName,
                    // Nye felter
                    channel = freqMhz?.let { calculateChannel(it) },
                    frequencyMhz = freqMhz,
                    hwMode = if (freqMhz != null && freqMhz < 5000) "g" else "a"
                )
            }.filter { it.bssid != "00:00:00:00:00:00" } // Filtrer ut "spøkelser"

            WifiScanResult(networks = res, state = WifiScanState.IDLE, success = true)
        } catch (e: Exception) {
            log.error("Feil ved parsing av IWD JSON: ${e.message}")
            WifiScanResult(WifiScanState.ERROR, false, networks = emptyList())
        }
    }

    // Hjelpefunksjon for å gjenbruke kanal-logikk
    private fun calculateChannel(f: Int): Int? = when {
        f in 2412..2484 -> ((f - 2412) / 5) + 1
        f in 5170..5825 -> ((f - 5170) / 5) + 34
        else -> null
    }

    override fun isSupported(): Boolean {
        // Sjekk direkte etter iwctl, som er klienten for iwd
        return java.io.File("/usr/bin/iwctl").exists()
    }

}