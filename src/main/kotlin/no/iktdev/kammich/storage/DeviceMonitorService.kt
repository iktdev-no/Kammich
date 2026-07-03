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

    private fun resolveInterfaceNode(sysPath: String): String {
        val root = File(sysPath)

        // Finn interface-noden (f.eks. 1-11:1.0)
        val iface = root.listFiles()?.firstOrNull {
            it.isDirectory && it.name.matches(Regex(".*:\\d+\\.\\d+"))
        }

        return iface?.absolutePath ?: sysPath
    }


    private fun identifyAndHandleDevice(sysPath: String) {
        try {
            val idVendor = File("$sysPath/idVendor").readText().trim()
            val idProduct = File("$sysPath/idProduct").readText().trim()

            val serialFile = File("$sysPath/serial")
            val serial = if (serialFile.exists()) serialFile.readText().trim() else "N/A"

            val busNum = File("$sysPath/busnum").readText().trim().toInt()
            val devNum = File("$sysPath/devnum").readText().trim().toInt()

            // Sjekk om dette er en lagringsenhet (rekursiv sysfs-sjekk)
            val isBlock = isBlockDevice(sysPath)
            log.info("Block sjekk ferdig")

            log.info("Enhet detektert: $idVendor:$idProduct (Block: $isBlock)")

            eventPublisher.publishEvent(
                DeviceDetectedEvent(
                    sysPath = sysPath,
                    vendor = idVendor,
                    product = idProduct,
                    serial = serial,
                    gphotoPort = "usb:%03d,%03d".format(busNum, devNum),
                    isBlockDevice = isBlock
                )
            )
        } catch (e: Exception) {
            log.error("Feil ved inspeksjon av enhet: ${e.message}", e)
        }
    }


    fun isBlockDevice(sysPath: String): Boolean {
        log.info("Kjører block sjekk via /sys/class/block")

        val blockClass = File("/sys/class/block")

        val timeoutMs = 10_000      // maks 10 sekunder
        val intervalMs = 1_000      // sjekk hvert sekund
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < timeoutMs) {
            val entries = blockClass.listFiles() ?: emptyArray()

            for (entry in entries) {
                val realPath = entry.canonicalPath

                if (realPath.contains(sysPath)) {
                    log.info("Block device funnet: ${entry.name} -> $realPath")
                    return true
                } else {
                    log.info("Block device: ${entry.name} -> $realPath, stemmer ikke med $sysPath")
                }
            }

            Thread.sleep(intervalMs.toLong())
        }

        log.info("Ingen block device funnet for $sysPath innen timeout")
        return false
    }






}