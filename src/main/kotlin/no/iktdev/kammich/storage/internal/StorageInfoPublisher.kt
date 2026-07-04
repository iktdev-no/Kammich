package no.iktdev.kammich.storage.internal

import no.iktdev.kammich.models.storage.Device
import no.iktdev.kammich.models.storage.StorageInfo
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.storage.DeviceService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class StorageInfoPublisher(
    private val deviceService: DeviceService,
    private val diskStorageService: DiskStorageService,
    private val smartCtlService: SmartCtlService,
    private val sseManager: SseManager
) {

    @Scheduled(fixedDelay = 100000)
    fun poll() {
        val devices = deviceService.getAllDevices()
            .map { getStorageInfo(it) }

        publish(devices)
    }

    fun getStorageInfo(device: Device): StorageInfo {
        val stats = diskStorageService.getStorageStats(device)
        val health = smartCtlService.getSMART(device.path).getOrThrow()
        return StorageInfo(stats, health)
    }

    fun publish(storage: List<StorageInfo>) {
        sseManager.send(
            mapOf(
                "type" to "storage-info-internal",
                "payload" to storage
            )
        )
    }
}
