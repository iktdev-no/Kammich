package no.iktdev.kammich.system.network.wifi

import no.iktdev.kammich.models.internal.network.asWifi
import no.iktdev.kammich.models.internal.network.setNetwork
import no.iktdev.kammich.models.internal.network.setTethering
import no.iktdev.kammich.models.shared.network.*
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.system.network.NetworkInterfaceRegistry
import no.iktdev.kammich.system.network.NetworkStateRepository
import no.iktdev.kammich.system.network.wifi.strategy.connection.WifiConnectionStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class WifiConnectivityService(
    private val sseManager: SseManager,
    private val strategies: List<WifiConnectionStrategy>,
    private val interfaceRegistry: NetworkInterfaceRegistry,
    private val repository: NetworkStateRepository
) {
    private val log = LoggerFactory.getLogger(WifiConnectivityService::class.java)

    init {
        updateSSE()
    }



    fun getCurrentState(): List<WifiNetworkConnection> {
        return getWifiClientInterfaces().map { it ->
            WifiNetworkConnection(
                it.name,
                network = it.connection?.network,
                state = it.connection?.state ?: InterfaceActiveState.Idle,
            )
        }
    }


    fun getWifiClientInterfaces(): List<WirelessInterface> {
        val ifaces = interfaceRegistry.getInterfaces(
            NetworkInterfaceType.Wifi,
            setOf(NetworkInterfaceMode.Client)
        )
            .map { (iface, state, isAvailable) ->
                val wifiState = state?.asWifi()
                WirelessInterface(
                    name = iface.interfaceName,
                    address = iface.macAdress,
                    isAvailable = isAvailable,
                    operatingState = wifiState?.toWirelessOperatingState() ?: WirelessOperatingState.Idle,
                    search = wifiState?.scanToWirelessNetworkSearch(),
                    connection = wifiState?.connectionToWirelessConnection(),
                    tethering = wifiState?.tetheringToWirelessTethering()
                )
            }
        return ifaces
    }


    fun getActiveStrategy(): WifiConnectionStrategy? {
        return strategies.find { it.isSupported() }
    }

    fun connectToNetwork(interfaceName: String, bssid: String, password: String?): Boolean {
        val networks = repository.getCurrentState().interfaces[interfaceName]?.asWifi()?.scan?.networks ?: emptyList()
        val network =
            networks.find { it.bssid == bssid } ?: throw IllegalArgumentException("No network found for $interfaceName")

        val strategy = getActiveStrategy()

        if (strategy == null) {
            log.error("Ingen støttet wifi-tilkoblingsmetode funnet! Vi har prøvd følgende:\n${strategies.joinToString("\n") { it.javaClass.simpleName }}")
            return false
        }

        val onReject = {
            log.error("Could not obtain lease to connect $interfaceName to network $network")
        }

        val result = interfaceRegistry.obtain(interfaceName, NetworkInterfaceMode.Client, onReject) { lease ->
            log.info("Connecting to $interfaceName with ssid ${network.ssid} using strategy: ${strategy::class.simpleName}")
            lease.setConnectionState(InterfaceActiveState.Connecting, network) {
                updateSSE()
            }
            log.info("Starter oppkobling til ${network.ssid} på $interfaceName")
            CompletableFuture.runAsync {
                val result = strategy.connect(interfaceName, network, password)
                lease.setConnectionState(result.state, network) {
                    updateSSE() // Push med en gang vi starter
                }
            }
        }
        return true
    }

    fun disconnectFromNetwork(interfaceName: String): Boolean {
        val strategy = getActiveStrategy()
            ?: return false

        val onReject = {
            log.error("Could not obtain lease to disconnect $interfaceName")
            updateSSE()
        }

        interfaceRegistry.obtain(interfaceName, NetworkInterfaceMode.Client, onReject) { lease ->
            log.info("Kobler fra $interfaceName")
            CompletableFuture.runAsync {
                val result = try {
                    strategy.disconnect(interfaceName)
                } catch (e: Exception) {
                    log.error("Could not disconnect $interfaceName", e)
                    null
                }
                lease.update({it.setNetwork(InterfaceActiveState.Idle, null)}) {
                    updateSSE()
                }
                lease.release()
            }
        }
        return true
    }

    fun getSSEPayload(): Map<String, Any> {
        val wifiInterfaces = repository.getCurrentState().interfaces
            .filterValues { it is no.iktdev.kammich.models.internal.network.WifiInterfaceState } // Filtrer først
            .mapValues { it.value as no.iktdev.kammich.models.internal.network.WifiInterfaceState } // Cast til rett type
            .map { (name, state) ->
                WifiNetworkConnection(
                    name = name,
                    state = state.state,
                    network = state.network,
                )
            }

        return mapOf(
            "type" to "wifi-connectivity",
            "payload" to wifiInterfaces // Dette er nå en List<WifiInterfaceState>
        )
    }

    private fun updateSSE() {
        val payload = getSSEPayload()
        log.info("SSE NMCLI payload: $payload")
        sseManager.send(payload)
    }
}