package no.iktdev.kammich.system.network

import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode.Client
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode.External
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode.Tether
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NetworkingService(
    private val registryV2: NetworkInterfaceRegistryV2,
    private val wifiConnectionServiceV2: WifiConnectionServiceV2,
    private val wifiTetherServiceV2: WifiTetherServiceV2,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun reset(nif: String) {
        val iface = registryV2.listNetworkInterfaces().find { it.interfaceName == nif } ?: run {
            log.error("No network interfaces found for $nif")
            return
        }
        when (iface.mode) {
            External, Client -> wifiConnectionServiceV2.disconnect(nif)
            Tether -> {
                wifiTetherServiceV2.stopTethering(nif)
                wifiTetherServiceV2.removeTetherDevice()
            }
            else -> {}
        }
        registryV2.forceReleaseAll(nif)
    }

}