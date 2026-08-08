package no.iktdev.kammich.system.network.wifi

import no.iktdev.kammich.models.internal.network.InterfaceState
import no.iktdev.kammich.models.internal.network.WifiInterfaceState
import no.iktdev.kammich.models.internal.network.asWifi
import no.iktdev.kammich.models.internal.network.setScan
import no.iktdev.kammich.models.shared.network.InterfaceActiveState
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiNetworkScan
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.SSEWifiScan
import no.iktdev.kammich.system.network.NetworkInterfaceRegistry
import no.iktdev.kammich.system.network.NetworkStateRepository
import no.iktdev.kammich.system.network.wifi.strategy.scan.FallbackScanStrategy
import no.iktdev.kammich.system.network.wifi.strategy.scan.WifiScanStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

@Component
class WifiScanner(
    private val sseManager: SseManager,
    private val repository: NetworkStateRepository,
    private val interfaceRegistry: NetworkInterfaceRegistry,
    private val strategies: List<WifiScanStrategy>,
) {

    private val log = LoggerFactory.getLogger(WifiScanner::class.java)


    private fun getActiveStrategy(): WifiScanStrategy? {
        return strategies.find { it.isSupported() }
    }

    /**
     * Trigger skanning fullstendig asynkront.
     */

    private val internalConcurrentState = ConcurrentHashMap<String, InterfaceActiveState>()

    fun triggerScanAsync(interfaceName: String) {
        if (internalConcurrentState[interfaceName] != InterfaceActiveState.Scanning) {
            log.info("WiFi-skanning kjører allerede på $interfaceName. Avbryter.")
            return
        }

        thread(start = true, name = "wifi-scan-$interfaceName") {
            log.info("Starter asynkron WiFi-skanning på $interfaceName...")
            internalConcurrentState[interfaceName] = InterfaceActiveState.Scanning
            getNetworks(interfaceName, forceRescan = true)
        }
    }


    /**
     * Kjører skanning, dytter gjennom jc, og caster til frontend-modeller.
     */
    fun getNetworks(interfaceName: String, forceRescan: Boolean): List<WifiNetwork> {
        val currentState = repository.getCurrentState().interfaces[interfaceName]
        if (!forceRescan && currentState != null && !isCacheStale(currentState)) {
            return currentState.asWifi()?.scan?.networks ?: emptyList()
        }

        val strategy = getActiveStrategy()
        if (strategy is FallbackScanStrategy) {
            log.error("Ingen forventet støttet wifi-scanner funnet! Vi har prøvd følgende:\n${strategies.filter{ it !is FallbackScanStrategy }.joinToString("\n") { it.javaClass.simpleName }}")
        } else if (strategy == null) {
            log.error("No strategy is supported for $interfaceName, we have tried:\n${strategies.filter { it !is FallbackScanStrategy }.joinToString("\n") { it.javaClass.simpleName }}")
            return emptyList()
        }
        log.info("Using strategy ${strategy::class.simpleName}")

        val result = interfaceRegistry.obtain(interfaceName, NetworkInterfaceMode.Client, { log.info("Unable to start scan! Unable to obtain lease")}) { lease ->
            lease.setState(InterfaceActiveState.Scanning) {
                updateSSE()
            }
            val result = strategy.scan(lease.getInterfaceName())
            val useResult = if (result.networks.size == 1) {
                val currentConnectedSSID = lease.getState()?.asWifi()?.network?.ssid
                if (currentConnectedSSID == result.networks.first().ssid) {
                    log.info("Using fallback strategy to acquire proper network scan")
                    strategies.filterIsInstance<FallbackScanStrategy>().firstOrNull()?.scan(lease.getInterfaceName()) ?: result
                } else result
            } else result

            lease.update({ it.setScan(InterfaceActiveState.Idle, useResult) }) {
                updateSSE()
            }
            useResult
        }
        internalConcurrentState[interfaceName] = InterfaceActiveState.Idle


        /*if (result.success && result.networks.isEmpty()) {
            val fallbackResult = strategies.find { it -> it is FallbackScanStrategy }?.scan(interfaceName)
            if (fallbackResult != null && fallbackResult.networks.isNotEmpty()) {
                result = fallbackResult
            }
        }
        registryOld.scanLastScans[interfaceName] = ZonedDateTime.now()

        val out = when (result.success) {
            true -> {
                registryOld.scanCurrentStates[interfaceName] = WifiScanState.IDLE
                result.networks
            }
            false -> {
                log.error("Scan error: ${result.message}")
                registryOld.scanCurrentStates[interfaceName] = WifiScanState.ERROR
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

        registryOld.scanResults[interfaceName] = cleaned
        updateSSE()*/
        return result?.networks ?: emptyList()
    }

    private fun isCacheStale(interfaceState: InterfaceState): Boolean {
        val last = interfaceState.asWifi()?.scan?.performedAt ?: return true
        return last.isBefore(ZonedDateTime.now().minusMinutes(5))
    }

    fun getSSEPayload(): SSEWifiScan {
        val wifiInterfaces = repository.getCurrentState().interfaces
            .filterValues { it is WifiInterfaceState } // Filtrer først
            .mapValues { it.value as WifiInterfaceState } // Cast til rett type
            .map { (name, state) ->
                WifiNetworkScan(
                    name = name,
                    state = state.state,
                    networks = state.scan?.networks ?: emptyList(),
                )
            }

        return SSEWifiScan(wifiInterfaces)
    }

    private fun updateSSE() {
        sseManager.send(getSSEPayload())
    }
}