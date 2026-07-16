package no.iktdev.kammich.system.network.wifi

import no.iktdev.kammich.models.internal.config.VirtualTetherDevice
import no.iktdev.kammich.models.shared.network.InterfaceRole
import no.iktdev.kammich.models.shared.network.WifiInterface
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
    fun getInterfaces(vararg roles: InterfaceRole = arrayOf(InterfaceRole.CLIENT, InterfaceRole.DUAL)): List<WifiInterfaceInfo> {
        return getInternalIwList().map { iw ->
            // Her kan du legge til den faktiske kapasitet-sjekken
            val caps = getCapabilities(iw.phyLink)
            val role = if (iw.name.endsWith("_ap")) InterfaceRole.AP else {
                if (caps?.isConcurrent == true) InterfaceRole.DUAL else InterfaceRole.CLIENT
            }
            WifiInterfaceInfo(
                interfaceName = iw.name,
                hardwareName = iw.phyLink,
                supportsAp = caps?.supportsAP ?: false,
                supportsApAndStationSimultaneously = caps?.isConcurrent ?: false,
                deviceId = iw.deviceId,
                role = role,
            )
        }.filter { it.role in roles }
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

    fun getInterfaceOfDevice(deviceId: String): WifiInterfaceInfo? {
        return getInterfaces().find { it.deviceId == deviceId }
    }

    fun ensureVirtualInterfaceExists(parentInterface: String, virtualName: String): VirtualTetherDevice {
        val existingInterfaces = getInternalIwList()

        // Sjekk om det allerede finnes, hent da eksisterende MAC
        val existing = existingInterfaces.find { it.name == virtualName }

        log.info("Oppretter virtuelt interface: $virtualName")
        val (parentMac, newMac) = generateVirtualMac(parentInterface)

        if (existing != null) {
            return VirtualTetherDevice(true, newMac, parentMac, virtualName)
        }



        wifiRunner.run(
            "sudo", "iw", "dev", parentInterface, "interface", "add",
            virtualName, "type", "__ap", "addr", newMac
        )

        wifiRunner.run("sudo", "ip", "link", "set", virtualName, "up")

        log.info("Virtuelt interface $virtualName opprettet med MAC $newMac")
        return VirtualTetherDevice(true, newMac, parentMac, virtualName)
    }

    private fun generateVirtualMac(parentInterface: String): Pair<String, String> {
        // 1. Hent den originale MAC-adressen fra filsystemet
        val originalMac = File("/sys/class/net/$parentInterface/address").readText().trim()

        // 2. Splitt i bytes
        val bytes = originalMac.split(":").map { it.toInt(16) }.toIntArray()

        // 3. Inkrementer siste byte (med modulo 256 for å unngå overflow)
        bytes[5] = (bytes[5] + 1) % 256

        // 4. Formater tilbake til MAC-format (f.eks. "XX:XX:XX:XX:XX:XX")
        return originalMac to bytes.joinToString(":") { "%02x".format(it) }
    }

    private data class IW(
        val name: String,
        val phyLink: String,
        val deviceId: String
    )
}