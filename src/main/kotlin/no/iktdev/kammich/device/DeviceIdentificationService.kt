package no.iktdev.kammich.device

import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.gphoto2.IGPhoto2
import no.iktdev.kammich.models.internal.DeviceReadyEvent
import no.iktdev.kammich.models.internal.SysPathReady
import no.iktdev.kammich.models.internal.UsbInterview
import no.iktdev.kammich.models.shared.device.BlockDevice
import no.iktdev.kammich.models.shared.device.DeviceType
import no.iktdev.kammich.models.shared.device.GPhoto2Device
import no.iktdev.kammich.system.LsblkService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.io.File

@Service
class DeviceIdentificationService(
    private val eventPublisher: ApplicationEventPublisher,
    private val configService: ConfigService,
    private val lsblkService: LsblkService,
    private val gphoto2: IGPhoto2
) {
    private val log = LoggerFactory.getLogger(DeviceIdentificationService::class.java)

    @EventListener(SysPathReady::class)
    fun onSysPathReady(event: SysPathReady) {
        log.info("Device interview started on $event")
        val result = interview(event.path)
        val device = when (result.getDeviceType()) {
            DeviceType.MTP, DeviceType.PTP -> assembleGPhoto2Device(result)
            DeviceType.BLOCK -> assembleBlockDevice(result)
            else -> {
                log.warn("Device type ${result.getDeviceType()} not supported. Raw info: manufacturer={}, product={}, config={}",
                    result.manufacturer, result.productName, result.configuration)
                if (configService.getConfig().assignUnknownDeviceAsBlockDevice) {
                    log.warn("Unstable override - Assigning unknown device as BlockDevice")
                    assembleBlockDevice(result)
                } else {
                    log.warn("Device type ${result.getDeviceType()} not supported. $result")
                    null
                }
            }
        }
        device?.let { d ->
            eventPublisher.publishEvent(DeviceReadyEvent(d))
        }
    }

    fun interview(sysPath: String): UsbInterview {
        val root = File(sysPath)

        return UsbInterview(
            sysPath = sysPath,
            manufacturer = readSafe(root, "manufacturer"),
            productName = readSafe(root, "product"),
            idProduct = readSafe(root, "idProduct"),
            idVendor = readSafe(root, "idVendor"),
            sn = readSafe(root, "serial").ifBlank { null },
            configuration = readSafe(root, "configuration"),
            busPath = sysPath
        )
    }

    fun assembleGPhoto2Device(i: UsbInterview): GPhoto2Device {
        val port = gphoto2.getPort(i.sysPath)
        val info = gphoto2.getDeviceInfo(port) // Dette er din "gullgruve"

        return GPhoto2Device(
            // Bruk SN fra GPhoto hvis mulig, ellers fall tilbake til USB-intervjuet
            id = info.summary.serialNumber ?: i.sn ?: "${i.idVendor}.${i.idProduct}",

            // Foretrekk det brukervennlige navnet fra kameraet
            name = info.summary.friendlyDeviceName ?: i.productName,

            // Foretrekk modell fra GPhoto, fall tilbake til USB-produkt
            model = info.summary.model ?: i.productName,

            // Foretrekk produsent fra GPhoto
            manufacturer = info.summary.manufacturer ?: i.manufacturer,

            sn = info.summary.serialNumber ?: i.sn ?: "UNKNOWN",

            port = port,
            type = i.getDeviceType(), // Bruk din smarte metode fra USBInterview
            sysPath = i.sysPath,

            // De nye rike feltene
            storage = info.summary.storageDevices
        )
    }

    fun assembleBlockDevice(i: UsbInterview): BlockDevice? {
        // 1. Finn fysisk disk (rot-node for blokkenheten)
        val candidates = File("/sys/class/block").listFiles { it.canonicalPath.startsWith(i.sysPath) }
        val physicalDiskEntry = candidates?.firstOrNull { entry ->
            File(entry, "devtype").let { if (it.exists()) it.readText().trim() == "disk" else !entry.name.matches(Regex(".*\\d$")) }
        } ?: return null

        val physicalDiskPath = "/dev/${physicalDiskEntry.name}"

        // 2. Bruk lsblk-tjenesten for å hente monteringsdetaljer
        val allMountPoints = lsblkService.getAllMountPoints(physicalDiskPath)
        val mountedSource = allMountPoints.firstOrNull { it.mounted }

        // 3. Robust ID-generering (prioriterer seriell, deretter fallback til hardware-info)
        val finalSn = mountedSource?.serialNumber?.takeIf { it.isNotBlank() }
            ?: i.sn?.takeIf { it != "UNKNOWN" && it.isNotBlank() }
            ?: "SN-${i.idVendor}-${i.idProduct}"

        // 4. Mappe til BlockDevice
        // Vi bruker substringBeforeLast('/') for å sikre at vi får rot-mappen
        // (f.eks. /run/kammich/removable/SD_MMC_MS_PRO i stedet for /run/.../sdc1)
        val cleanMountPoint = mountedSource?.mountPoint?.substringBeforeLast('/')

        return BlockDevice(
            id = finalSn,
            name = mountedSource?.modelName ?: i.productName,
            model = mountedSource?.modelName ?: "Unknown",
            manufacturer = i.manufacturer,
            sn = mountedSource?.serialNumber ?: i.sn ?: "UNKNOWN",
            mountPoint = cleanMountPoint,
            devicePath = physicalDiskPath,
            sysPath = i.sysPath
        )
    }


    private fun readSafe(root: File, fileName: String): String {
        val file = File(root, fileName)
        return if (file.exists()) {
            file.readText().trim()
        } else {
            "UNKNOWN"
        }
    }
}