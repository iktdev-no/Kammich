package no.iktdev.kammich.storage.provider

import no.iktdev.kammich.gphoto2.GPhoto2
import no.iktdev.kammich.gphoto2.IGPhoto2
import no.iktdev.kammich.gphoto2.model.GPhoto2File
import no.iktdev.kammich.gphoto2.model.GPhoto2NodeType
import no.iktdev.kammich.models.files.KFile
import no.iktdev.kammich.models.files.KFileType
import no.iktdev.kammich.models.storage.removable.Device
import no.iktdev.kammich.storage.DeviceMonitorService
import no.iktdev.kammich.storage.DiskCacheService
import no.iktdev.kammich.toMD5
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.servlet.function.RequestPredicates.path
import java.io.File

@Service
class GPhoto2StorageProvider(
    private val diskCacheService: DiskCacheService,
    private val gPhoto2: IGPhoto2
): StorageProvider {
    private val log = LoggerFactory.getLogger(DeviceMonitorService::class.java)

    override fun listFiles(device: Device, path: String?): List<KFile> {
        if (device.path.isNullOrBlank()) {
            log.info("No path on device to g photo2 storage provider found")
            return emptyList()
        }
        val files = gPhoto2.getFiles(device.path, path ?: "")
        return files.map { it.toKFile(device) }
    }

    override fun getThumbnails(folder: KFile): List<File> {
        TODO("Not yet implemented")
    }


    fun GPhoto2File.toKFile(device: Device): KFile {
        return KFile(
            id = "$folderPath/$name".toMD5(),
            name = name,
            path = folderPath,
            size = sizeBytes,
            device = device,
            type = when (type) { GPhoto2NodeType.FILE -> {
                KFileType.FILE
            } GPhoto2NodeType.FOLDER -> {
                KFileType.DIRECTORY
            } }
        )
    }
}