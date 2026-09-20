package no.iktdev.kammich.storage.internal

import jakarta.annotation.PostConstruct
import no.iktdev.kammich.models.shared.storage.Transport
import no.iktdev.kammich.models.shared.storage.DiskHealth
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.SSEStorageHealth
import no.iktdev.kammich.system.LsblkService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class DiskHealthService(
    private val diskService: LsblkService,
    private val smartService: SmartCtlService,
    private val sseManager: SseManager
) {
    private val log = LoggerFactory.getLogger(DiskHealthService::class.java)

    // Vi cacher nå helsen for alle disker
    private val healthCache = mutableMapOf<String, DiskHealth>()

    @PostConstruct
    fun init() {
        log.info("Kjører første helsesjekk ved oppstart...")
        runHealthCheck()
    }

    @Scheduled(cron = "0 0 * * * *") // Sjekk hver time
    fun runHealthCheck() {
        val health = diskService.getAllPhysicalDevices().map { device ->
            smartService.getSMART(device.path)
                .onSuccess { health ->
                    healthCache[device.path] = health
                    if (!health.isHealthy) {
                        triggerAlert("Kritisk helse på ${health.deviceName}")
                    } else {
                        log.info("Disk sjekket: ${device.path} - OK")
                    }
                }
                .onFailure { e ->
                    // Her logger du feilen, men loopen fortsetter til neste disk!
                    log.error("Kunne ikke hente SMART-data for ${device.path}: ${e.message}")
                }
        }
        val hr = health.mapNotNull { it.getOrNull() }
        sseManager.send(SSEStorageHealth(payload = hr))
    }

    private fun triggerAlert(message: String) {
        log.error("🚨 $message")
        // Her kan du legge til varsling i fremtiden
    }

}