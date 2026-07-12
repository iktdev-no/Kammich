package no.iktdev.kammich.system

import jakarta.annotation.PostConstruct
import no.iktdev.kammich.models.internal.SysPathReady
import no.iktdev.kammich.models.internal.SysPathRemoved
import no.iktdev.kammich.models.internal.UdevEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class UdevService(
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(UdevService::class.java)

    @PostConstruct
    fun startMonitoring() {
        log.info("Starting Udev monitoring service.")
        log.info("Listening for usb subsystem matches..")

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
                                    eventPublisher.publishEvent(SysPathReady("/sys${udevEvent.path}"))
                                }
                            }
                            "remove" -> {
                                eventPublisher.publishEvent(SysPathRemoved("/sys${udevEvent.path}"))
                            }
                        }
                    }
                }
            }
        }.start()
    }

    /**
     * Parser rå udev-linjer til et formatert UdevEvent.
     * Filtrerer ut sub-interfaces (f.eks. 1-1:1.0) og fokuserer på USB-hovedenheter.
     */
    fun parseUdevEvent(line: String): UdevEvent? {
        // 1. Splittingen er grei, men vi må håndtere at del 3 inneholder både path og info
        val parts = line.split(Regex("\\s+"), limit = 4)

        if (parts.size < 3 || !parts[0].startsWith("KERNEL[")) {
            return null
        }

        val event = parts[1].replace(":", "")
        // 2. Vi henter ut path ved å ta alt frem til første mellomrom (eller parentes)
        val rawPath = parts[2]

        // 3. Validering av path
        val isUsb = rawPath.contains("/usb")
        val deviceNode = rawPath.substringAfterLast("/")
        val isMainDevice = !deviceNode.contains(":")

        if (isUsb && isMainDevice) {
            log.debug("Relevant USB-event oppdaget: {} på path {}", event, rawPath)
            return UdevEvent(event, rawPath)
        }

        return null
    }
}