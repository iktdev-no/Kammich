package no.iktdev.kammich.storage

import jakarta.annotation.PostConstruct
import no.iktdev.kammich.models.storage.DeviceDetectedEvent
import no.iktdev.kammich.models.storage.UdevEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.io.File

@Service
class DeviceMonitorService(
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(DeviceMonitorService::class.java)

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
                        }
                    }
                }
            }
        }.start()
    }


    private fun identifyAndHandleDevice(sysPath: String) {
        try {
            // Vent litt på at filene skal bli tilgjengelige (idVendor etc)
            val idVendor = readWithRetry("$sysPath/idVendor") ?: throw Exception("Kunne ikke lese idVendor")
            val idProduct = File("$sysPath/idProduct").readText().trim()
            val serial = File("$sysPath/serial").let { if (it.exists()) it.readText().trim() else "N/A" }
            val busNum = File("$sysPath/busnum").readText().trim().toInt()
            val devNum = File("$sysPath/devnum").readText().trim().toInt()

            // JUSTERING: Sjekk om dette er Mass Storage før vi starter den tunge while-loopen
            val isMassStorage = isMassStorageDevice(sysPath)
            val isBlock = if (isMassStorage) isBlockDevice(sysPath) else false

            log.info("Enhet detektert: $idVendor:$idProduct (MassStorage: $isMassStorage, Block: $isBlock)")

            eventPublisher.publishEvent(DeviceDetectedEvent(
                sysPath = sysPath, vendor = idVendor, product = idProduct,
                serial = serial, gphotoPort = "usb:%03d,%03d".format(busNum, devNum),
                isBlockDevice = isBlock
            ))
        } catch (e: Exception) {
            log.error("Feil ved inspeksjon av enhet: ${e.message}")
        }
    }

    private fun isMassStorageDevice(sysPath: String): Boolean {
        // Ser etter "bInterfaceClass" == 08 i USB-hierarkiet (Mass Storage)
        return File(sysPath).walkTopDown().maxDepth(2).any { file ->
            file.name == "bInterfaceClass" && file.readText().trim() == "08"
        }
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