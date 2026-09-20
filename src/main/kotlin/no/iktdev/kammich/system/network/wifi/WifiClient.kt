package no.iktdev.kammich.system.network.wifi

import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode
import no.iktdev.kammich.models.shared.network.WifiInterfaceErrorType
import no.iktdev.kammich.models.shared.network.WifiInterfaceState
import no.iktdev.kammich.models.shared.network.WifiInterfaceStateType
import no.iktdev.kammich.system.network.CaptivePortal
import no.iktdev.kammich.system.network.NetworkInterfaceRegistryV2
import no.iktdev.kammich.system.network.strategy.connection.WifiConnectionStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class WifiClient(
    private val registry: NetworkInterfaceRegistryV2,
    private val strategies: List<WifiConnectionStrategy>,
    private val networkScan: WifiScanServiceV2,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun findStrategy(): WifiConnectionStrategy? =
        strategies.find { it.isSupported() }

    fun connect(
        ifName: String,
        bssid: String,
        password: String?,
        listener: WifiClientConnectEvents
    ) {
        val strategy = findStrategy()
            ?: throw WifiClientStrategyNotFoundException(
                "No strategy for $ifName found!"
            )

        val lease = registry.acquire(
            interfaceName = ifName,
            requestedMode = NetworkInterfaceMode.Client,
            onReject = { listener.onLeaseRejected() }
        ) ?: return

        val network = networkScan
            .getNetworks(ifName)
            .find { it.bssid == bssid }
            ?: run {
                lease.release()
                listener.onNetworkNotFound()
                return
            }

        val state = try {
            strategy.connect(
                lease.interfaceName,
                network,
                password
            )
        } catch (e: Exception) {
            log.error("Feil ved tilkobling til $bssid på $ifName", e)
            lease.release()
            listener.onError(e)
            return
        }

        listener.onResult(state)

        if (state.state !in setOf(
                WifiInterfaceStateType.Connected,
                WifiInterfaceStateType.Connecting
            )
        ) {
            registry.releaseLease(lease.interfaceName, NetworkInterfaceMode.Client) {}
            lease.release()
        }
    }

    fun disconnect(
        ifName: String,
        listener: WifiClientDisconnectEvents
    ) {
        val strategy = findStrategy()
            ?: throw WifiClientStrategyNotFoundException(
                "No strategy for $ifName found!"
            )

        try {
            registry.releaseLease(
                interfaceName = ifName,
                mode = NetworkInterfaceMode.Client
            ) {
                log.info("Kobler fra $ifName")

                try {
                    strategy.disconnect(ifName)
                    listener.onResult()
                } catch (e: Exception) {
                    if (e.message?.contains("not active", ignoreCase = true) == true) {
                        log.info("$ifName var allerede frakoblet")
                        listener.onResult()
                    } else {
                        throw e
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Frakobling feilet for $ifName", e)
            listener.onError(e)
        }
    }

    interface WifiClientConnectEvents {
        fun onLeaseRejected()
        fun onNetworkNotFound()
        fun onResult(state: WifiInterfaceState)
        fun onError(error: Exception)
    }

    interface WifiClientDisconnectEvents {
        fun onResult()
        fun onError(error: Exception)
    }

    open class WifiClientException(msg: String) : Exception(msg)
    class WifiClientStrategyNotFoundException(msg: String) :
        WifiClientException(msg)
}