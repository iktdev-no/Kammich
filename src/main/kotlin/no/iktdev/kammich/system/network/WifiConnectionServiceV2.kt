package no.iktdev.kammich.system.network

import no.iktdev.kammich.models.shared.network.*
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.networking.SSEWifiConnection
import no.iktdev.kammich.system.network.strategy.connection.WifiConnectionStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class WifiConnectionServiceV2(
    private val sseManager: SseManager,
    private val interfaceRegistry: NetworkInterfaceRegistryV2,
    private val strategies: List<WifiConnectionStrategy>,
    private val scanServiceV2: WifiScanServiceV2
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun getActiveStrategy(): WifiConnectionStrategy? = strategies.find { it.isSupported() }

    fun connectAsync(interfaceName: String, bssid: String, password: String?) {
        log.debug("Connecting to $interfaceName")
        val interfacePresent = interfaceRegistry.findInterface(interfaceName)
        if (interfacePresent == null) {
            throw IllegalStateException("Interface $interfaceName is not present.")
        }

        CompletableFuture.runAsync {
            connect(interfaceName, bssid, password)
        }
    }

    fun connect(interfaceName: String, bssid: String, password: String?): WifiConnection {
        val strategy = getActiveStrategy() ?: run {
            log.error("Ingen støttet tilkoblingsstrategi funnet.")
            val errResult = WifiConnection(interfaceName, WifiConnectionStateType.Disconnected, null, WifiInterfaceClientError.Unknown)
            send(errResult)
            return errResult
        }

        val network = scanServiceV2.getNetworks(interfaceName)
            .find { it.bssid == bssid }

        if (network == null) {
            log.error("Could not find network $bssid")
            val failureResult = WifiConnection(interfaceName, WifiConnectionStateType.Disconnected, null, WifiInterfaceClientError.NetworkNotFound)
            send(failureResult)
            return failureResult
        }

        // Send connecting state via riktig SSE-wrapper
        send(WifiConnection(ifName = interfaceName, state = WifiConnectionStateType.Connecting, network))

        // Bruk acquire direkte for å opprette/få tak i den vedvarende leasen
        val lease = interfaceRegistry.acquire(
            interfaceName = interfaceName,
            requestedMode = NetworkInterfaceMode.Client,
            onReject = { log.warn("Klarte ikke å skaffe lease for tilkobling på $interfaceName") }
        ) ?: run {
            val failureResult = WifiConnection(interfaceName, WifiConnectionStateType.Disconnected, null, WifiInterfaceClientError.Unknown)
            send(failureResult)
            return failureResult
        }

        var connectResult = WifiConnection(interfaceName, WifiConnectionStateType.Disconnected, null, WifiInterfaceClientError.Unknown)

        try {
            log.info("Kobler til $bssid på ${lease.interfaceName}")
            connectResult = strategy.connect(lease.interfaceName, network, password)

            // Hvis tilkoblingen feilet med en gang, kan vi frigi leasen igjen med en gang
            if (connectResult.state != WifiConnectionStateType.Connected && connectResult.state != WifiConnectionStateType.Connecting) {
                lease.release()
            }
        } catch (e: Exception) {
            log.error("Feil ved tilkobling til $bssid på $interfaceName", e)
            lease.release()
            connectResult = WifiConnection(
                interfaceName,
                WifiConnectionStateType.Disconnected,
                null,
                WifiInterfaceClientError.Unknown
            )
        }

        send(connectResult)

        return connectResult
    }

    fun disconnect(interfaceName: String): Boolean {
        val strategy = getActiveStrategy() ?: return false
        val network = strategy.getNetwork(interfaceName)
        send(WifiConnection(ifName = interfaceName, state = WifiConnectionStateType.Disconnecting, network))

        var success = false
        try {
            interfaceRegistry.releaseLease(interfaceName, NetworkInterfaceMode.Client) { lease ->
                log.info("Kobler fra $interfaceName")
                try {
                    strategy.disconnect(interfaceName)
                    success = true
                } catch (e: Exception) {
                    val errorMsg = e.message ?: ""
                    if (errorMsg.contains("not active", ignoreCase = true)) {
                        log.info("Enheten $interfaceName var allerede frakoblet på systemnivå. Regner som suksess.")
                        success = true
                    } else {
                        log.error("Strategi-disconnect feilet for $interfaceName", e)
                        throw e
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Frakobling feilet med exception", e)
        }

        send(WifiConnection(ifName = interfaceName, state = WifiConnectionStateType.Disconnected, null))
        return success
    }

    fun send(state: WifiConnection) {
        if (state.state == WifiConnectionStateType.Connected && state.network == null) {
            log.error("Sending null network!")
        }
        sseManager.send(SSEWifiConnection(state.ifName,state))
    }


    /**
     * Henter en oversikt over trådløse klient-grensesnitt (tilsvarende V1 getWifiClientInterfaces).
     */
    fun getCurrentState(): List<WifiInterfaceClient> {
        val clientStrategy = getActiveStrategy()

        return interfaceRegistry.getInterfaces(
            NetworkInterfaceType.Wifi,
            setOf(NetworkInterfaceMode.Client, NetworkInterfaceMode.Idle)
        ).map { (nif, isAvailable) ->
            val name = nif.interfaceName
            val caps = (nif as? WirelessNetworkInterface)?.caps ?: emptySet()
            val clientState = clientStrategy?.getState(name) ?: WifiConnection(ifName = name, state = WifiConnectionStateType.Disconnected, network = null)
            val network = if (clientStrategy != null && clientState.state == WifiConnectionStateType.Connected) {
                clientStrategy.getNetwork(name)
            } else {
                null
            }

            WifiInterfaceClient(
                name = name,
                isUsable = isAvailable,
                operatingMode = nif.mode, // Eller tilsvarende felt for faktisk kjøretidsmodus
                state = clientState.state,
                network = network,
                caps = caps
            )
        }
    }
}