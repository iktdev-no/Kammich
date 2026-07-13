package no.iktdev.kammich.system.network.wifi

import no.iktdev.kammich.models.shared.network.WifiConnectionResult
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiConnectivityState
import no.iktdev.kammich.models.shared.network.WifiInterfaceState
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.system.network.wifi.connectivity.WifiConnectionStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class WifiConnectivityService(
    private val sseManager: SseManager,
    private val scanner: WifiScanner,
    private val runner: WifiRunner,
    private val interfaces: WifiInterfaces,
    private val strategies: List<WifiConnectionStrategy>
) {
    private val log = LoggerFactory.getLogger(WifiConnectivityService::class.java)

    // Lagrer tilstand per interface
    private val states = ConcurrentHashMap<String, WifiConnectivityState>()
    private val activeSsids = ConcurrentHashMap<String, String>()

    init {
        interfaces.getInterfaces().forEach { interfaceName ->
            states[interfaceName.interfaceName] = WifiConnectivityState.IDLE
        }
        refreshState()
    }

    fun refreshState() {
        val activeStrategy = getActiveStrategy() ?: return

        states.keys.forEach { interfaceName ->
            val actualState = activeStrategy.getState(interfaceName)

            // Oppdater vår interne cache med virkeligheten
            states[interfaceName] = actualState.connectivityState
            if (actualState.network != null) {
                activeSsids[interfaceName] = actualState.network.ssid
            } else {
                activeSsids.remove(interfaceName)
            }
        }
        // Push den oppdaterte sannheten til alle via SSE
        updateSSE()
    }


    fun getCurrentState(interfaceName: String): WifiConnectivityState =
        states.getOrDefault(interfaceName, WifiConnectivityState.IDLE)

    fun getCurrentNetwork(interfaceName: String): WifiNetwork? {
        val ssid = activeSsids[interfaceName] ?: return null
        // Bruk scannerens nye interface-spesifikke resultat
        return getActiveStrategy()?.getState(interfaceName)?.network ?: scanner.getCurrentScanResult(interfaceName).find { it.ssid == ssid }
    }



    fun getAllNetworkStates(): List<WifiInterfaceState> {
        val activeStrategy = getActiveStrategy()
        return states.keys.mapNotNull { iface ->
            activeStrategy?.getState(iface)
        }
    }

    private fun getActiveStrategy(): WifiConnectionStrategy? {
        return strategies.find { it.isSupported() }
    }

    fun connectToNetwork(interfaceName: String, ssid: String, password: String?): WifiConnectionResult {
        val strategy = getActiveStrategy()

        if (strategy == null) {
            log.error("Ingen støttet wifi-tilkoblingsmetode funnet! Vi har prøvd følgende:\n${strategies.joinToString("\n") { it.javaClass.simpleName }}")
            return WifiConnectionResult(false, "Ingen tilkoblingsmetode støttet", WifiConnectivityState.FAILED)
        }
        log.info("Connecting to $interfaceName with ssid $ssid using strategy: ${strategy::class.simpleName}")

        states[interfaceName] = WifiConnectivityState.CONNECTING
        updateSSE() // Push med en gang vi starter

        log.info("Starter oppkobling til $ssid på $interfaceName")

        val result = strategy.connect(interfaceName, ssid, password)
        states[interfaceName] = result.status
        activeSsids[interfaceName] = ssid
        updateSSE() // Push med en gang vi starter
        return result
    }

    fun disconnectFromNetwork(interfaceName: String): WifiConnectionResult {
        val strategy = getActiveStrategy()
            ?: return WifiConnectionResult(false, "Ingen støttet metode", WifiConnectivityState.FAILED)

        log.info("Kobler fra $interfaceName")

        val result = strategy.disconnect(interfaceName)

        if (result.success) {
            states[interfaceName] = WifiConnectivityState.DISCONNECTED
            activeSsids.remove(interfaceName)
            updateSSE()
        }

        return result
    }

    fun getSSEPayload(): Map<String, Any> {
        // Lag en liste av alle interface-tilstander
        val allInterfaceStates = states.keys.map { iface ->
            WifiInterfaceState(
                interfaceName = iface,
                connectivityState = getCurrentState(iface),
                network = getCurrentNetwork(iface) // Du kan også mappe til hele WifiNetwork her
            )
        }

        return mapOf(
            "type" to "wifi-connectivity",
            "payload" to allInterfaceStates // Dette er nå en List<WifiInterfaceState>
        )
    }

    private fun updateSSE() {
        sseManager.send(getSSEPayload())
    }
}