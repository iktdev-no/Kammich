package no.iktdev.kammich.storage

import jakarta.annotation.PostConstruct
import no.iktdev.kammich.models.storage.DeviceType
import no.iktdev.kammich.models.storage.internal.BlockDeviceDetectedEvent
import no.iktdev.kammich.models.storage.internal.DeviceDetectedEvent
import no.iktdev.kammich.models.storage.internal.DeviceRemovedEvent
import no.iktdev.kammich.models.storage.internal.MTPDeviceDetectedEvent
import no.iktdev.kammich.models.storage.internal.PTPDeviceDetectedEvent
import no.iktdev.kammich.models.storage.internal.UdevEvent
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Service
class DeviceMonitorService(
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(DeviceMonitorService::class.java)
    private val activeDevices = ConcurrentHashMap<String, DeviceDetectedEvent>()

    // Funksjon for testing: Tar en rå tekstlinje og returnerer et event hvis det er en hovedenhet
    fun parseUdevEvent(line: String): UdevEvent? {
        val parts = line.split(Regex("\\s+"))
        if (parts.size < 3 || !parts[0].startsWith("KERNEL[")) return null

        val event = parts[1]
        val path = parts[2]

        // Vi henter ut siste del av stien (f.eks. "1-11:1.0" eller "1-11")
        val lastPart = path.substringAfterLast("/")

        // Vi sjekker om 'usb' er i stien, OG at siste del (enhetsnavnet) IKKE har kolon
        if (path.contains("usb") && !lastPart.contains(":")) {
            return UdevEvent(event, path)
        }
        return null
    }

    @PostConstruct
    fun startMonitoring() {
        log.info("Starting DeviceMonitorService")

        // Vi må kjøre denne i en egen tråd for at Spring skal starte ferdig
        Thread {
            val process = ProcessBuilder("udevadm", "monitor", "--kernel", "--subsystem-match=usb")
                .start()

            val readyToAdd = mutableSetOf<String>()
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val udevEvent = parseUdevEvent(line)
                    if (udevEvent != null) {
                        when (udevEvent.event) {
                            "add" -> readyToAdd.add(udevEvent.path)
                            "bind" -> {
                                if (readyToAdd.remove(udevEvent.path)) { // .remove returnerer true hvis den fantes
                                    log.info("+++ HOVEDENHET KLAR: ${udevEvent.path} +++")
                                    identifyAndHandleDevice("/sys${udevEvent.path}")
                                }
                            }
                            "remove" -> {
                                val removedDevice = activeDevices.remove(udevEvent.path)
                                if (removedDevice != null) {
                                    log.info("--- ENHET FJERNET: ${udevEvent.path} ---")
                                    // Send event til FE om at den er borte
                                    eventPublisher.publishEvent(DeviceRemovedEvent(removedDevice.sysPath))
                                }
                            }
                        }
                    }
                }
            }
        }.start()
    }

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        log.info("Applikasjon klar, starter rescan...")
        rescanExistingDevices()
    }
    fun rescanExistingDevices() {
        log.info("Scanning for already connected devices...")
        val usbDir = File("/sys/bus/usb/devices")
        usbDir.listFiles { _, name -> name.matches(Regex("\\d+-\\d+(\\.\\d+)*")) }?.forEach { deviceDir ->
            // Sjekk om dette er en hovedenhet (uten kolon i navnet)
            if (!deviceDir.name.contains(":")) {
                val sysPath = deviceDir.canonicalPath
                log.info("Found existing device: $sysPath")
                identifyAndHandleDevice(sysPath)
            }
        }
    }

    private fun identifyAndHandleDevice(sysPath: String) {
        try {
            val idVendor = readWithRetry("$sysPath/idVendor")!!
            val idProduct = File("$sysPath/idProduct").readText().trim()
            val serial = File("$sysPath/serial").let { if (it.exists()) it.readText().trim() else "N/A" }

            val type = detectDeviceType(sysPath)

            val event = when (type) {

                DeviceType.BLOCK -> {
                    val blockDev = findBlockDevice(sysPath) ?: "/dev/unknown"
                    BlockDeviceDetectedEvent(
                        sysPath = sysPath,
                        vendor = idVendor,
                        product = idProduct,
                        serial = serial,
                        devicePath = blockDev
                    )
                }

                DeviceType.PTP -> {
                    val port = buildUsbPort(sysPath)
                    PTPDeviceDetectedEvent(
                        sysPath = sysPath,
                        vendor = idVendor,
                        product = idProduct,
                        serial = serial,
                        devicePath = port
                    )
                }

                DeviceType.MTP -> {
                    val port = buildUsbPort(sysPath)
                    MTPDeviceDetectedEvent(
                        sysPath = sysPath,
                        vendor = idVendor,
                        product = idProduct,
                        serial = serial,
                        devicePath = port
                    )
                }
            }

            eventPublisher.publishEvent(event)

        } catch (e: Exception) {
            log.error("Feil ved inspeksjon av enhet: ${e.message}")
        }
    }



    fun detectDeviceType(sysPath: String): DeviceType {
        // BLOCK
        if (isMassStorageDevice(sysPath) && isBlockDevice(sysPath)) {
            return DeviceType.BLOCK
        }

        // PTP/MTP
        val usbType = detectPtpOrMtp(sysPath)
        if (usbType != null) {
            return usbType
        }

        // Default fallback
        return DeviceType.PTP
    }

    fun buildUsbPort(sysPath: String): String {
        val bus = File("$sysPath/busnum").readText().trim().toInt()
        val dev = File("$sysPath/devnum").readText().trim().toInt()
        return "usb:%03d,%03d".format(bus, dev)
    }


    fun detectPtpOrMtp(sysPath: String): DeviceType? {
        File(sysPath).walkTopDown().maxDepth(3).forEach { file ->
            if (file.name == "bInterfaceClass" && file.readText().trim() == "06") {
                val parent = file.parentFile
                val sub = File("${parent}/bInterfaceSubClass").readText().trim()
                val proto = File("${parent}/bInterfaceProtocol").readText().trim()

                if (sub == "01") {
                    return when (proto) {
                        "01" -> DeviceType.PTP
                        "00" -> DeviceType.MTP
                        else -> null
                    }
                }
            }
        }
        return null
    }



    private fun isMassStorageDevice(sysPath: String): Boolean {
        // Ser etter "bInterfaceClass" == 08 i USB-hierarkiet (Mass Storage)
        return File(sysPath).walkTopDown().maxDepth(2).any { file ->
            file.name == "bInterfaceClass" && file.readText().trim() == "08"
        }
    }

    fun findBlockDevice(sysPath: String): String? {
        val blockDir = File("/sys/class/block")
        val entries = blockDir.listFiles() ?: return null

        for (entry in entries) {
            if (entry.canonicalPath.contains(sysPath)) {
                return "/dev/${entry.name}"
            }
        }
        return null
    }


    fun isBlockDevice(sysPath: String): Boolean {
        log.info("Venter på blokkenhet for $sysPath...")
        val blockDir = File("/sys/class/block")
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < 5000) { // 5 sek er nok for disk
            val entries = blockDir.listFiles() ?: emptyArray()
            for (entry in entries) {
                if (entry.canonicalPath.contains(sysPath)) {
                    log.info("Block device funnet: ${entry.name}")
                    return true
                }
            }
            Thread.sleep(500)
        }
        return false
    }

    private fun readWithRetry(path: String, retries: Int = 3): String? {
        repeat(retries) {
            val file = File(path)
            if (file.exists()) return file.readText().trim()
            Thread.sleep(200)
        }
        return null
    }


}