package no.iktdev.kammich.system.network.al

import no.iktdev.kammich.models.shared.network.WirelessNetworkInterfaceCapability
import no.iktdev.kammich.system.SysCommand
import no.iktdev.kammich.system.network.v1.wifi.parser.WifiPhyInfoParser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class IwAL(private val exec: SysCommand): IIwAL {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun getPhysicalInterfaces(ifName: String): String? {
        val out = exec.nonSudo("iw", "dev", ifName, "info")
            .getOrNull() ?: return null
        return out.lines().map { it.trim() }.find { it.startsWith("wiphy") }
            ?.split(Regex("\\s+")) // Splitter på alle typer whitespace (space, tab, etc.)
            ?.lastOrNull()         // Henter ut det siste elementet (tallet)
            ?.let { "phy$it" }     // Konverterer til "phy0"

    }

    override fun getWirelessCapabilities(phy: String): Set<WirelessNetworkInterfaceCapability> {
        val phyOut = exec.nonSudo("iw", "phy", phy, "info").getOrNull() ?: run {
            log.info("Could not find phy network interface {}", phy);
            return emptySet()
        }
        return WifiPhyInfoParser().parseCapabilities(phyOut)
    }
}