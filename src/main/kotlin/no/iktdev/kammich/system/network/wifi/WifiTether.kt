package no.iktdev.kammich.system.network.wifi

import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode
import no.iktdev.kammich.models.shared.network.WifiInterfaceState
import no.iktdev.kammich.models.shared.network.WifiInterfaceStateType
import no.iktdev.kammich.services.ConfigService
import no.iktdev.kammich.system.network.NetworkInterfaceRegistryV2
import no.iktdev.kammich.system.network.strategy.ap.AccessPointStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class WifiTether(
    private val registry: NetworkInterfaceRegistryV2,
    private val strategies: List<AccessPointStrategy>,
    private val configService: ConfigService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun findStrategy(): AccessPointStrategy? =
        strategies.find { it.isSupported() }

    fun start(
        ifName: String,
        listener: WifiTetherStartEvents
    ) {
        val strategy = findStrategy()
            ?: throw WifiTetherStrategyNotFoundException(
                "No strategy for $ifName found!"
            )

        val lease = registry.acquire(
            interfaceName = ifName,
            requestedMode = NetworkInterfaceMode.Tether,
            onReject = {
                listener.onLeaseRejected()
            }
        ) ?: return

        try {
            val state = strategy.start(
                lease.interfaceName,
                configService.getConfig().tetherSetting,
                autoconnect = true
            )

            listener.onResult(state)

            if (state.state !in setOf(WifiInterfaceStateType.Tethering, WifiInterfaceStateType.Starting)) {
                lease.release()
            }
        } catch (e: Exception) {
            log.error("Feil ved start av AP på $ifName", e)

            lease.release()
            listener.onError(e)
        }
    }

    fun stop(
        ifName: String,
        listener: WifiTetherStopEvents
    ) {
        val strategy = findStrategy()
            ?: throw WifiTetherStrategyNotFoundException("No strategy for $ifName found!")

        try {
            registry.releaseLease(
                interfaceName = ifName,
                mode = NetworkInterfaceMode.Tether
            ) {
                try {
                    strategy.stop(ifName)
                    listener.onResult()
                } catch (e: Exception) {
                    if (e.message?.contains("not active", ignoreCase = true) == true) {
                        log.info("$ifName var allerede inaktiv")
                        listener.onResult()
                    } else {
                        throw e
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Stopp av AP feilet for $ifName", e)
            listener.onError(e)
        }
    }

    interface WifiTetherStartEvents {
        fun onLeaseRejected()
        fun onResult(state: WifiInterfaceState)
        fun onError(error: Exception)
    }

    interface WifiTetherStopEvents {
        fun onResult()
        fun onError(error: Exception)
    }

    open class WifiTetherException(msg: String) : Exception(msg)

    class WifiTetherStrategyNotFoundException(msg: String) :
        WifiTetherException(msg)
}