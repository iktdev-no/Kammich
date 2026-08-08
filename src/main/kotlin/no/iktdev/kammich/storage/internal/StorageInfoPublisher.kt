package no.iktdev.kammich.storage.internal

import no.iktdev.kammich.models.shared.storage.Transport
import no.iktdev.kammich.models.shared.storage.LsblkBlockDevice
import no.iktdev.kammich.models.shared.storage.StorageInfo
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.SSEStorageInfoInternal
import no.iktdev.kammich.system.LsblkService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class StorageInfoPublisher(
    private val lsblkService: LsblkService,
    private val diskStorageService: DiskStorageService,
    private val smartCtlService: SmartCtlService,
    private val sseManager: SseManager
) {
    private val log = LoggerFactory.getLogger(StorageInfoPublisher::class.java)

    private lateinit var storageInfo: List<StorageInfo>

    init {
        poll()
    }

    @Scheduled(fixedDelay = 100000)
    fun poll() {
        storageInfo = lsblkService.getAllPhysicalDevices(Transport.NVME, Transport.SATA)
            .mapNotNull { getStorageInfo(it) }
        publish()
    }

    fun getStorageInfo(lsblkBlockDevice: LsblkBlockDevice): StorageInfo? {
        val stats = diskStorageService.getStorageStats(lsblkBlockDevice)
        val health = smartCtlService.getSMART(lsblkBlockDevice.path)
        if (!health.isSuccess) {
            if (lsblkBlockDevice.transport in listOf(Transport.USB, Transport.UNKNOWN)) {
                log.debug("{} does not support SMART over {}", lsblkBlockDevice.serialNumber, lsblkBlockDevice.transport)
                return null
            }
        }
        return StorageInfo(stats, health.getOrThrow())
    }

    fun getPayload() = SSEStorageInfoInternal(storageInfo)

    fun publish() {
        sseManager.send(getPayload())
    }
}
