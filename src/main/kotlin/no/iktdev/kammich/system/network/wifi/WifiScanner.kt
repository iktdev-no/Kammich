package no.iktdev.kammich.system.network.wifi

import no.iktdev.kammich.models.internal.network.IwScanItem
import no.iktdev.kammich.models.shared.network.WifiConnectionResult
import no.iktdev.kammich.models.shared.network.WifiConnectivityState
import no.iktdev.kammich.models.shared.network.WifiInterfaceScanState
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiScanState
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.system.network.wifi.connectivity.WifiConnectionStrategy
import no.iktdev.kammich.system.network.wifi.parser.WifiScanResultParser
import no.iktdev.kammich.system.network.wifi.scan.FallbackScanStrategy
import no.iktdev.kammich.system.network.wifi.scan.WifiScanStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

@Component
class WifiScanner(
    private val scanResultParser: WifiScanResultParser,
    private val wifiRunner: WifiRunner,
    private val sseManager: SseManager,
    private val strategies: List<WifiScanStrategy>
) {

    private val log = LoggerFactory.getLogger(WifiScanner::class.java)

    private val lastScans = ConcurrentHashMap<String, ZonedDateTime>()
    private val scanResults = ConcurrentHashMap<String, List<WifiNetwork>>()
    private val currentStates = ConcurrentHashMap<String, WifiScanState>()

    private fun getActiveStrategy(): WifiScanStrategy? {
        return strategies.find { it.isSupported() }
    }

    fun getCurrentState(interfaceName: String): WifiScanState =
        currentStates.getOrDefault(interfaceName, WifiScanState.IDLE)

    fun getCurrentScanResult(interfaceName: String): List<WifiNetwork> {
        return scanResults.getOrDefault(interfaceName, emptyList())
    }

    /**
     * Trigger skanning fullstendig asynkront.
     */
    fun triggerScanAsync(interfaceName: String) {
        if (getCurrentState(interfaceName) == WifiScanState.SCANNING) {
            log.info("WiFi-skanning kjører allerede på $interfaceName. Avbryter.")
            return
        }

        thread(start = true, name = "wifi-scan-$interfaceName") {
            log.info("Starter asynkron WiFi-skanning på $interfaceName...")
            getNetworks(interfaceName, forceRescan = true)
        }
    }

    /**
     * Kjører skanning, dytter gjennom jc, og caster til frontend-modeller.
     */
    fun getNetworks(interfaceName: String, forceRescan: Boolean): List<WifiNetwork> {
        if (forceRescan || lastScans[interfaceName] == null || isCacheStale(interfaceName)) {
            currentStates[interfaceName] = WifiScanState.SCANNING
            updateSSE()
        } else return getCurrentScanResult(interfaceName)

        val strategy = getActiveStrategy()
        if (strategy is FallbackScanStrategy) {
            log.error("Ingen forventet støttet wifi-scanner funnet! Vi har prøvd følgende:\n${strategies.filter{ it !is FallbackScanStrategy }.joinToString("\n") { it.javaClass.simpleName }}")
        } else if (strategy == null) {
            log.error("No strategy is supported for $interfaceName, we have tried:\n${strategies.filter { it !is FallbackScanStrategy }.joinToString("\n") { it.javaClass.simpleName }}")
            return emptyList()
        }
        log.info("Using strategy ${strategy::class.simpleName}")

        val result = strategy.scan(interfaceName)
        lastScans[interfaceName] = ZonedDateTime.now()

        val out = when (result.success) {
            true -> {
                currentStates[interfaceName] = WifiScanState.IDLE
                result.networks
            }
            false -> {
                log.error("Scan error: ${result.message}")
                currentStates[interfaceName] = WifiScanState.ERROR
                emptyList()
            }
        }.sortedByDescending { it.signalPercent }
        log.info("WiFi scan result: $out")
        val cleaned = if (out.any { it.bssid == "00:00:00:00:00:00" }) {
            out.map { net ->
                if (net.bssid == "00:00:00:00:00" && net.isHidden) {
                    net.copy(bssid = "unknown-${net.ssid}-${net.bssid}-${net.frequencyMhz}")
                } else net
            }
        } else out

        scanResults[interfaceName] = cleaned
        updateSSE()
        return cleaned
    }

    private fun isCacheStale(interfaceName: String): Boolean {
        val last = lastScans[interfaceName] ?: return true
        return last.isBefore(ZonedDateTime.now().minusMinutes(5))
    }

    fun getSSEPayload(): Map<String, Any> {
        val allInterfaceStates = currentStates.keys.map { iface ->
            val networks = scanResults[iface].takeIf { it?.isNotEmpty() == true }
                ?: emptyList()
            WifiInterfaceScanState(
                interfaceName = iface,
                scanning = currentStates[iface] ?: WifiScanState.IDLE,
                networks = networks
            )
        }
        if (allInterfaceStates.any { it.scanning == WifiScanState.ERROR }) {
            log.error("Scanning state returned an error..")
        }
        return mapOf(
            "type" to "wifi-scan",
            "payload" to allInterfaceStates
        )
    }

    private fun updateSSE() {
        sseManager.send(getSSEPayload())
    }
}