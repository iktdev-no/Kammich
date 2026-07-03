package no.iktdev.kammich.storage.internal

import com.google.gson.Gson
import com.google.gson.JsonParser
import no.iktdev.kammich.models.storage.DiskHealth
import no.iktdev.kammich.models.storage.NvmeRoot
import no.iktdev.kammich.models.storage.SataRoot
import no.iktdev.kammich.storage.DeviceDiscoveryService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class DiskHealthService(
    private val discovery: DeviceDiscoveryService,
    private val smartService: SmartCtlService,
) {
    private val log = LoggerFactory.getLogger(DiskHealthService::class.java)

    // Vi cacher nå helsen for alle disker
    private val healthCache = mutableMapOf<String, DiskHealth>()

    @Scheduled(cron = "0 0 * * * *") // Sjekk hver time
    fun runHealthCheck() {
        discovery.getAvailableDisks().forEach { device ->
            smartService.getSMART(device.path)
                .onSuccess { health ->
                    healthCache[device.path] = health
                    if (!health.isHealthy) {
                        triggerAlert("Kritisk helse på ${health.deviceName}")
                    }
                    log.info("Disk sjekket: ${device.path} - OK")
                }
                .onFailure { e ->
                    // Her logger du feilen, men loopen fortsetter til neste disk!
                    log.error("Kunne ikke hente SMART-data for ${device.path}: ${e.message}")
                }
        }
    }

    private fun triggerAlert(message: String) {
        log.error("🚨 $message")
        // Her kan du legge til varsling i fremtiden
    }

}