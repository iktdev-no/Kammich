package no.iktdev.kammich.system.network.wifi

import no.iktdev.kammich.immich.services.ImmichContextService
import no.iktdev.kammich.models.shared.network.*
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.networking.SSEWifiState
import no.iktdev.kammich.system.network.CaptivePortal
import no.iktdev.kammich.system.network.NetworkInterfaceRegistryV2
import no.iktdev.kammich.system.network.strategy.connection.WifiConnectionStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class WifiConnectionServiceV2(
    private val sseManager: SseManager,
    private val interfaceRegistry: NetworkInterfaceRegistryV2,
    private val strategies: List<WifiConnectionStrategy>,
    private val scanServiceV2: WifiScanServiceV2,
    private val captivePortal: CaptivePortal,
    private val immichContextService: ImmichContextService
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
            immichContextService.initializeAndVerifyContext()
        }
    }

    fun connect(
        interfaceName: String,
        bssid: String,
        password: String?
    ): WifiInterfaceState {
        val strategy = getActiveStrategy() ?: run {
            log.error("Ingen støttet tilkoblingsstrategi funnet.")

            val result = WifiInterfaceState(
                ifName = interfaceName,
                state = WifiInterfaceStateType.Disconnected,
                operatingMode = NetworkInterfaceMode.Client,
                error = WifiInterfaceErrorType.Unknown
            )

            send(result)
            return result
        }

        val network = scanServiceV2.getNetworks(interfaceName)
            .find { it.bssid == bssid }

        if (network == null) {
            log.error("Could not find network $bssid")

            val result = WifiInterfaceState(
                ifName = interfaceName,
                state = WifiInterfaceStateType.Disconnected,
                operatingMode = NetworkInterfaceMode.Client,
                error = WifiInterfaceErrorType.ClientNetworkNotFound
            )

            send(result)
            return result
        }

        send(
            WifiInterfaceState(
                ifName = interfaceName,
                state = WifiInterfaceStateType.Connecting,
                operatingMode = NetworkInterfaceMode.Client,
                network = network
            )
        )

        val lease = interfaceRegistry.acquire(
            interfaceName = interfaceName,
            requestedMode = NetworkInterfaceMode.Client,
            onReject = {
                log.warn(
                    "Klarte ikke å skaffe lease for tilkobling på $interfaceName"
                )
            }
        ) ?: run {
            val result = WifiInterfaceState(
                ifName = interfaceName,
                state = WifiInterfaceStateType.Disconnected,
                operatingMode = NetworkInterfaceMode.Client,
                error = WifiInterfaceErrorType.Unknown
            )

            send(result)
            return result
        }

        val connectResult = try {
            log.info("Kobler til $bssid på ${lease.interfaceName}")

            val result = strategy.connect(
                lease.interfaceName,
                network,
                password
            )

            if (result.state == WifiInterfaceStateType.Connected) {
                val captiveStatus = captivePortal.verify(lease.interfaceName)

                log.info(
                    "Captive portal sjekk fullført for ${lease.interfaceName}: " +
                            captiveStatus.state
                )
            }

            if (
                result.state != WifiInterfaceStateType.Connected &&
                result.state != WifiInterfaceStateType.Connecting
            ) {
                lease.release()
            }

            result
        } catch (e: Exception) {
            log.error(
                "Feil ved tilkobling til $bssid på $interfaceName",
                e
            )

            lease.release()

            WifiInterfaceState(
                ifName = interfaceName,
                state = WifiInterfaceStateType.Disconnected,
                operatingMode = NetworkInterfaceMode.Client,
                error = WifiInterfaceErrorType.Unknown
            )
        }

        send(connectResult)

        return connectResult
    }

    fun disconnect(interfaceName: String): Boolean {
        val strategy = getActiveStrategy() ?: return false
        val network = strategy.getNetwork(interfaceName)

        send(
            WifiInterfaceState(
                ifName = interfaceName,
                state = WifiInterfaceStateType.Disconnecting,
                operatingMode = NetworkInterfaceMode.Client,
                network = network
            )
        )

        var success = false

        try {
            interfaceRegistry.releaseLease(
                interfaceName,
                NetworkInterfaceMode.Client
            ) { lease ->
                log.info("Kobler fra $interfaceName")

                try {
                    strategy.disconnect(interfaceName)
                    success = true
                } catch (e: Exception) {
                    val errorMsg = e.message ?: ""

                    if (errorMsg.contains("not active", ignoreCase = true)) {
                        log.info(
                            "Enheten $interfaceName var allerede frakoblet på systemnivå. " +
                                    "Regner som suksess."
                        )
                        success = true
                    } else {
                        log.error(
                            "Strategi-disconnect feilet for $interfaceName",
                            e
                        )
                        throw e
                    }
                }
            }
        } catch (e: Exception) {
            log.error(
                "Frakobling feilet med exception",
                e
            )
        }

        send(
            WifiInterfaceState(
                ifName = interfaceName,
                state = WifiInterfaceStateType.Disconnected,
                operatingMode = NetworkInterfaceMode.Idle
            )
        )

        immichContextService.initializeAndVerifyContext()

        return success
    }

    fun send(state: WifiInterfaceState) {
        sseManager.send(SSEWifiState(state.ifName,state))
    }

}