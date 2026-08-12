package no.iktdev.kammich.models.internal

import no.iktdev.kammich.models.shared.device.DeviceInterfaceType
import org.slf4j.LoggerFactory
import java.io.File

data class UsbInterview(
    val sysPath: String,
    val manufacturer: String,
    val idProduct: String,
    val idVendor: String,
    val sn: String?,
    val configuration: String,
    val productName: String,
    val busPath: String
) {
    val isMassStorage: Boolean by lazy {
        // 1. Sjekk rot
        val rootClass = File("$sysPath/bInterfaceClass")
        if (rootClass.exists() && rootClass.readText().trim() == "08") return@lazy true

        // 2. Sjekk sub-noder (nødvendig for mange kortlesere/huber)
        File(sysPath).listFiles { it.isDirectory && it.name.contains(":") }
            ?.any { sub ->
                val subClass = File(sub, "bInterfaceClass")
                subClass.exists() && subClass.readText().trim() == "08"
            } ?: false
    }

    val isMtp: Boolean by lazy {
        val log = LoggerFactory.getLogger("UsbInterview")
        val subNodes = File(sysPath).listFiles { it.isDirectory && it.name.contains(":") }

        if (subNodes == null) {
            log.debug("Ingen sub-noder funnet for $sysPath")
            return@lazy false
        }

        val foundMatch = subNodes.any { sub ->
            val bClass = File(sub, "bInterfaceClass").takeIf { it.exists() }?.readText()?.trim()
            val bSubClass = File(sub, "bInterfaceSubClass").takeIf { it.exists() }?.readText()?.trim()
            val bProtocol = File(sub, "bInterfaceProtocol").takeIf { it.exists() }?.readText()?.trim()
            val interfaceName = File(sub, "interface").takeIf { it.exists() }?.readText()?.trim() ?: "unknown"

            log.info("Sjekker node {}: Class={}, SubClass={}, Protocol={}, InterfaceName={}",
                sub.name, bClass, bSubClass, bProtocol, interfaceName)

            // 1. Vendor specific (Class ff, SubClass ff) - ofte brukt av Samsung/Android
            val isVendorMtp = (bClass == "ff" && bSubClass == "ff")
            // 2. Standard Imaging class (Class 06, SubClass 01) - standard PTP/MTP
            val isStandardPtpMtp = (bClass == "06" && bSubClass == "01")

            isVendorMtp || isStandardPtpMtp
        }

        if (foundMatch) log.info("MTP-grensesnitt identifisert for enhet: $sysPath")
        foundMatch
    }

    fun getDeviceType(): DeviceInterfaceType {
        val c = configuration.lowercase()
        val p = productName.lowercase()

        return when {
            c.contains("mtp") || isMtp -> DeviceInterfaceType.MTP
            c.contains("ptp") || p.contains("ptp") -> DeviceInterfaceType.PTP
            c.contains("rndis") -> DeviceInterfaceType.NETWORK
            c.contains("midi") -> DeviceInterfaceType.AUDIO
            isMassStorage -> DeviceInterfaceType.BLOCK
            else -> DeviceInterfaceType.UNKNOWN
        }
    }
}
