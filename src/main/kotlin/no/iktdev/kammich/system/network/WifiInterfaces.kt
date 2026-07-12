package no.iktdev.kammich.system.network

import no.iktdev.kammich.models.shared.network.WifiInterfaceInfo
import no.iktdev.kammich.system.network.parser.WifiPhyInfoParser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File

@Component
class WifiInterfaces(
    private val wifiRunner: WifiRunner
) {
    private val log = LoggerFactory.getLogger(WifiInterfaces::class.java)

    /**
     * Henter alle aktive trådløse enheter direkte fra Linux-kjernen (/sys/class/net).
     */
    fun getInterfaces(): List<WifiInterfaceInfo> {
        return getInternalIwList().map { iw ->
            // Her kan du legge til den faktiske kapasitet-sjekken
            val caps = getCapabilities(iw.name)
            WifiInterfaceInfo(
                interfaceName = iw.name,
                hardwareName = iw.phyLink,
                supportsAp = caps?.supportsAP ?: false,
                supportsApAndStationSimultaneously = caps?.isConcurrent ?: false,
            )
        }
    }

    private fun getInternalIwList(): List<IW> {
        return File("/sys/class/net").takeIf { it.exists() && it.isDirectory }?.listFiles()?.mapNotNull { file ->
            if (File(file, "wireless").exists() || File(file, "phy80211").exists()) {
                val phyLink = File(file, "device/phy80211").takeIf { it.exists() }?.canonicalFile?.name ?: "phy0"
                IW(file.name, phyLink)
            } else null
        } ?: emptyList()
    }

    private fun getCapabilities(phy: String): WifiPhyInfoParser.WifiCapability? {
        val out = wifiRunner.run("iw", "phy", phy, "info")

        // Hvis output er tom, logg feilen og returner null (eller throw en custom exception)
        if (out.isBlank()) {
            log.error("Kunne ikke hente info for $phy: Runner returnerte tom streng.")
            return null
        }

        return try {
            WifiPhyInfoParser().parse(out)
        } catch (e: Exception) {
            log.error("Parseren feilet for $phy. Output var: ${out.take(50)}...", e)
            null
        }
    }

    private data class IW(val name: String, val phyLink: String)
}