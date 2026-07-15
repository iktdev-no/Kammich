package no.iktdev.kammich.system.network.wifi

import no.iktdev.kammich.models.shared.network.WifiConnectionResult
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.ConnectivityState
import no.iktdev.kammich.models.shared.network.WifiInterfaceState
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.system.network.wifi.strategy.connection.WifiConnectionStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class WifiConnectivityService(
    private val sseManager: SseManager,
    private val scanner: WifiScanner,
    private val runner: WifiRunner,
    private val interfaces: WifiInterfaces,
    private val strategies: List<WifiConnectionStrategy>,
    private val registry: WifiStateRegistry,
    private val wifiTetherService: WifiTetherService
) {
    private val log = LoggerFactory.getLogger(WifiConnectivityService::class.java)

    init {
        interfaces.getInterfaces().forEach { interfaceName ->
            registry.connectivityCurrentStates[interfaceName.interfaceName] = ConnectivityState.IDLE
        }
        refreshState()
    }

    fun refreshState() {
        val activeStrategy = getActiveStrategy() ?: return

        registry.connectivityCurrentStates.keys.forEach { interfaceName ->
            val actualState = activeStrategy.getState(interfaceName)

            // Oppdater vår interne cache med virkeligheten
            registry.connectivityCurrentStates[interfaceName] = actualState.connectivityState
            if (actualState.network != null) {
                registry.connectivityCurrentNetworks[interfaceName] = actualState.network
            } else {
                registry.connectivityCurrentNetworks.remove(interfaceName)
            }
        }
        // Push den oppdaterte sannheten til alle via SSE
        updateSSE()
    }


    fun getCurrentState(interfaceName: String): ConnectivityState =
        registry.connectivityCurrentStates.getOrDefault(interfaceName, ConnectivityState.IDLE)

    fun getCurrentNetwork(interfaceName: String): WifiNetwork? {
        return registry.connectivityCurrentNetworks[interfaceName] ?:
            getActiveStrategy()?.getState(interfaceName)?.network
    }



    fun getAllNetworkStates(): List<WifiInterfaceState> {
        val activeStrategy = getActiveStrategy()
        return registry.connectivityCurrentStates.keys.mapNotNull { iface ->
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
            return WifiConnectionResult(false, "Ingen tilkoblingsmetode støttet", ConnectivityState.FAILED)
        }
        log.info("Connecting to $interfaceName with ssid $ssid using strategy: ${strategy::class.simpleName}")

        registry.connectivityCurrentStates[interfaceName] = ConnectivityState.CONNECTING
        updateSSE() // Push med en gang vi starter

        log.info("Starter oppkobling til $ssid på $interfaceName")

        val result = strategy.connect(interfaceName, ssid, password)
        registry.connectivityCurrentStates[interfaceName] = result.status
        getCurrentNetwork(interfaceName)?.let {
            registry.connectivityCurrentNetworks[interfaceName] = it
        }

        updateSSE() // Push med en gang vi starter
        return result
    }

    fun disconnectFromNetwork(interfaceName: String): WifiConnectionResult {
        val strategy = getActiveStrategy()
            ?: return WifiConnectionResult(false, "Ingen støttet metode", ConnectivityState.FAILED)

        log.info("Kobler fra $interfaceName")

        val result = strategy.disconnect(interfaceName)

        registry.connectivityCurrentStates[interfaceName] = result.status

        if (result.success) {
            registry.connectivityCurrentNetworks.remove(interfaceName)
        } else {
            log.error("Struggling to disconnect $interfaceName from its wifi", result.message)
        }
        updateSSE()

        return result
    }

    fun getSSEPayload(): Map<String, Any> {
        // Lag en liste av alle interface-tilstander
        val allInterfaceStates = registry.connectivityCurrentStates.keys.map { iface ->
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