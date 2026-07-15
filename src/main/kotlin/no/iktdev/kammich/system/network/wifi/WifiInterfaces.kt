package no.iktdev.kammich.system.network.wifi

import no.iktdev.kammich.models.shared.network.WifiInterfaceInfo
import no.iktdev.kammich.system.network.wifi.parser.WifiPhyInfoParser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File

@Component
class WifiInterfaces(
    private val wifiRunner: WifiRunner,
) {
    private val log = LoggerFactory.getLogger(WifiInterfaces::class.java)
        /**
     * Henter alle aktive trådløse enheter direkte fra Linux-kjernen (/sys/class/net).
     */
    fun getInterfaces(): List<WifiInterfaceInfo> {
        return getInternalIwList().map { iw ->
            // Her kan du legge til den faktiske kapasitet-sjekken
            val caps = getCapabilities(iw.phyLink)
            WifiInterfaceInfo(
                interfaceName = iw.name,
                hardwareName = iw.phyLink,
                supportsAp = caps?.supportsAP ?: false,
                supportsApAndStationSimultaneously = caps?.isConcurrent ?: false,
                deviceId = iw.deviceId
            )
        }
    }

    private fun getInternalIwList(): List<IW> {
        return File("/sys/class/net").takeIf { it.exists() && it.isDirectory }?.listFiles()?.mapNotNull { file ->
            if (File(file, "wireless").exists() || File(file, "phy80211").exists()) {
                val phyLink = File(file, "device/phy80211").takeIf { it.exists() }?.canonicalFile?.name ?: "phy0"
                val macAddress = File(file, "address").readText().trim()
                IW(file.name, phyLink, macAddress)
            } else null
        } ?: emptyList()
    }

    private fun getCapabilities(phy: String): WifiPhyInfoParser.WifiCapability? {
        return wifiRunner.run("iw", "phy", phy, "info")
            .map { WifiPhyInfoParser().parse(it) }
    }

    private data class IW(val name: String, val phyLink: String, val deviceId: String)
}