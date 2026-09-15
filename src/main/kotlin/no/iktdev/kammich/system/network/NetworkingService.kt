package no.iktdev.kammich.system.network

import no.iktdev.kammich.models.shared.network.NetworkInterface
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode.Client
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode.External
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode.Tether
import no.iktdev.kammich.models.shared.network.NetworkInterfaceType.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NetworkingService(
    private val registryV2: NetworkInterfaceRegistryV2,
    private val wifiConnectionServiceV2: WifiConnectionServiceV2,
    private val wifiTetherServiceV2: WifiTetherServiceV2,
    private val ethernetConnectionService: EthernetConnectionService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun reset(nif: String) {
        val iface = registryV2.listNetworkInterfaces().find { it.interfaceName == nif } ?: run {
            log.error("No network interfaces found for $nif")
            return
        }
        when (iface.type) {
            Ethernet -> handleEthernetReset(iface)
            Wifi -> handleWifiReset(iface)
        }
    }

    private fun handleWifiReset(iface: NetworkInterface) {
        when (iface.mode) {
            External, Client -> {
                wifiConnectionServiceV2.disconnect(iface.interfaceName)
            }
            Tether -> {
                wifiTetherServiceV2.stopTethering(iface.interfaceName)
                wifiTetherServiceV2.removeTetherDevice()
            }
            else -> {}
        }
        registryV2.forceReleaseAll(iface.interfaceName)
    }

    private fun handleEthernetReset(iface: NetworkInterface) {
        ethernetConnectionService.reset(iface.interfaceName)
    }

}