package no.iktdev.kammich.storage

import jakarta.annotation.PostConstruct
import no.iktdev.kammich.models.storage.UdevEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class DeviceMonitorService {
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
        val process = ProcessBuilder("udevadm", "monitor", "--kernel", "--subsystem-match=usb")
            .start()

        Thread {
            val readyToAdd = mutableSetOf<String>()
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val udevEvent = parseUdevEvent(line)

                    if (udevEvent != null) {
                        when (udevEvent.event) {
                            "add" -> readyToAdd.add(udevEvent.path)
                            "bind" -> {
                                if (readyToAdd.contains(udevEvent.path)) {
                                    log.info("+++ HOVEDENHET KLAR: ${udevEvent.path} +++")
                                    readyToAdd.remove(udevEvent.path)
                                    handleDeviceAsync(udevEvent.path)
                                }
                            }
                        }
                    }
                }
            }
        }.start()
    }

    private fun handleDeviceAsync(path: String) {
        Thread {
            Thread.sleep(200)
            identifyAndHandleDevice("/sys$path")
        }.start()
    }

    private fun identifyAndHandleDevice(sysPath: String) {
        try {
            val idVendor = File("$sysPath/idVendor").readText().trim()
            val idProduct = File("$sysPath/idProduct").readText().trim()

            val serialFile = File("$sysPath/serial")
            val serial = if (serialFile.exists()) serialFile.readText().trim() else "N/A"

            // Hent bus og device nummer for gphoto2-port
            val busNum = File("$sysPath/busnum").readText().trim().toInt()
            val devNum = File("$sysPath/devnum").readText().trim().toInt()
            val gphotoPort = "usb:%03d,%03d".format(busNum, devNum)

            log.info("Ny enhet funnet! Vendor: $idVendor, Product: $idProduct, Serial: $serial, GPhotoPort: $gphotoPort")

            // Eksempel: Kjør en kommando for å verifisere kameraet

        } catch (e: Exception) {
            log.error("Kunne ikke lese enhet eller prosess feilet: ${e.message}")
        }
    }
}