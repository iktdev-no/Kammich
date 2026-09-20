package no.iktdev.kammich.storage.internal

import com.google.gson.Gson
import no.iktdev.kammich.services.ConfigService
import no.iktdev.kammich.models.shared.storage.LsblkBlockDevice
import no.iktdev.kammich.models.shared.storage.MediaStats
import no.iktdev.kammich.models.shared.storage.StorageStats
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.SSEStorageStatsMedia
import no.iktdev.kammich.system.LsblkService
import no.iktdev.kammich.utils.DiskUtils
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File

@Service
class DiskStorageService(
    private val configService: ConfigService,
    private val lsblkService: LsblkService,
    private val sse: SseManager
) {
    private lateinit var mediaStorageStats: MediaStats
    private val log = LoggerFactory.getLogger(javaClass)


    @Scheduled(fixedDelay = 100000)
    fun poll() {
        mediaStorageStats = getMediaStorageStats()
        publish()
    }

    fun getStorageStats(
        lsblkBlockDevice: LsblkBlockDevice
    ): StorageStats {
        val mountPoint = lsblkBlockDevice.mountPoint

        if (mountPoint == null) {
            return StorageStats(
                totalBytes = 0,
                freeBytes = 0,
                usableBytes = 0,
                isMounted = false
            )
        }

        val file = File(mountPoint)

        return StorageStats(
            totalBytes = file.totalSpace,
            freeBytes = file.freeSpace,
            usableBytes = file.usableSpace,
            isMounted =
                file.exists() &&
                        file.isDirectory &&
                        file.canRead()
        )
    }

    fun getMediaStorageStats(): MediaStats {
        val mediaPath = File(configService.getConfig().mediaPath)

        // 1. Finn disk-info
        val allDevices = lsblkService.getAllPhysicalDevices()
        log.info("Found devices ${Gson().toJson(allDevices)}")
        val device = allDevices.find {
            it.mountPoint != null && mediaPath.absolutePath.startsWith(it.mountPoint)
        }

        // 2. Beregn bytes
        val total = mediaPath.totalSpace
        val free = mediaPath.freeSpace
        val used = total - free

        // 3. Tell filer (Aggregert)
        var photos = 0L
        var videos = 0L

        if (mediaPath.exists()) {
            mediaPath.walk()
                .maxDepth(3) // Begrens dybde for ytelse
                .filter { it.isFile }
                .forEach { file ->
                    when (file.extension.lowercase()) {
                        "jpg", "jpeg", "png", "heic", "webp" -> photos++
                        "mp4", "mov", "avi", "mkv" -> videos++
                    }
                }
        }


        return MediaStats(
            totalBytes = total,
            freeBytes = free,
            usedBytes = used,
            percentUsed = if (total > 0) (used.toDouble() / total.toDouble()) * 100 else 0.0,
            photoCount = photos,
            videoCount = videos,
            transport = device?.transport?.name ?: "Unknown"
        )
    }


    fun getPayload() = SSEStorageStatsMedia(
    mediaStorageStats
    )

    fun publish() {
        sse.send(getPayload())
    }
}