package no.iktdev.kammich.storage.internal

import no.iktdev.kammich.models.shared.Transport
import no.iktdev.kammich.models.shared.storage.BlockDevice
import no.iktdev.kammich.models.shared.storage.StorageInfo
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.storage.DeviceService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class StorageInfoPublisher(
    private val deviceService: DeviceService,
    private val diskStorageService: DiskStorageService,
    private val smartCtlService: SmartCtlService,
    private val sseManager: SseManager
) {
    private val log = LoggerFactory.getLogger(StorageInfoPublisher::class.java)

    private lateinit var storageInfo: List<StorageInfo>

    @Scheduled(fixedDelay = 100000)
    fun poll() {
        storageInfo = deviceService.getAllDevices()
            .mapNotNull { getStorageInfo(it) }
        publish()
    }

    fun getStorageInfo(blockDevice: BlockDevice): StorageInfo? {
        val stats = diskStorageService.getStorageStats(blockDevice)
        val health = smartCtlService.getSMART(blockDevice.path)
        if (!health.isSuccess) {
            if (blockDevice.transport in listOf(Transport.USB, Transport.UNKNOWN)) {
                log.debug("{} does not support SMART over {}", blockDevice.serialNumber, blockDevice.transport)
                return null
            }
        }
        return StorageInfo(stats, health.getOrThrow())
    }

    fun getPayload(): Map<String, Any> {
        return mapOf(
            "type" to "storage-info-internal",
            "payload" to storageInfo
        )
    }

    fun publish() {
        sseManager.send(getPayload())
    }
}
