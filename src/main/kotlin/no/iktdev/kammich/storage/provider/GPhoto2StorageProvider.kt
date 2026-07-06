package no.iktdev.kammich.storage.provider

import no.iktdev.kammich.gphoto2.IGPhoto2
import no.iktdev.kammich.gphoto2.model.GPhoto2File
import no.iktdev.kammich.gphoto2.model.GPhoto2NodeType
import no.iktdev.kammich.models.shared.files.KFile
import no.iktdev.kammich.models.shared.files.KFileType
import no.iktdev.kammich.models.shared.storage.removable.Device
import no.iktdev.kammich.storage.DeviceMonitorService
import no.iktdev.kammich.storage.DiskCacheService
import no.iktdev.kammich.toMD5
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
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

    override fun listAllFiles(device: Device, path: String?): List<KFile> {
        val results = mutableListOf<KFile>()
        val root = path ?: ""

        fun crawl(currentPath: String) {
            val files = try {
                listFiles(device, currentPath)
            } catch (e: Exception) {
                log.warn("Kunne ikke lese mappe $currentPath: ${e.message}")
                return
            }

            for (file in files) {
                if (file.type == KFileType.DIRECTORY) {
                    // VIKTIG: Bruk file.path direkte!
                    // GPhoto2/KFile-logikken din forventer å gå inn i denne stien
                    if (file.path != currentPath) { // Unngå evig løkke hvis provider returnerer seg selv
                        crawl(file.path)
                    }
                } else {
                    results.add(file)
                }
            }
        }

        crawl(root)
        return results
    }

    override fun getDCIM(device: Device): KFile? {
        fun search(currentPath: String, depth: Int): KFile? {
            if (depth >= 3) return null

            val files = try {
                listFiles(device, currentPath)
            } catch (e: Exception) {
                log.warn("Kunne ikke lese mappe $currentPath: ${e.message}")
                return null
            }

            // 1. Sjekk om vi finner DCIM i nåværende liste
            val dcim = files.find { it.type == KFileType.DIRECTORY && it.name.equals("DCIM", ignoreCase = true) }
            if (dcim != null) {
                return dcim
            }

            // 2. Hvis ikke, dykk ned i undermapper ved å bruke it.path direkte
            return files.filter { it.type == KFileType.DIRECTORY }
                .mapNotNull { folder ->
                    // VIKTIG: Bruk it.path direkte, ikke kombiner med name
                    search(folder.path, depth + 1)
                }
                .firstOrNull()
        }

        return search("", 0)
    }

    override fun getThumbnails(folder: KFile, recurse: Boolean): List<File> {
        log.info("Get thumbnails for device ${folder.device.name}")
        val cachePath = try {
            diskCacheService.getCacheDirectory(folder.device.id)
        } catch (ex: Exception) {
            log.error(ex.message)
            return emptyList()
        }
        log.info("Using $cachePath")
        // 1. Vi ber gphoto2 synkronisere hele mappa i EN operasjon
        // Dette er lynraskt fordi USB-tilkoblingen holdes åpen
        gPhoto2.getThumbnails(cachePath, folder.device.path!!, folder.path, recurse)

        // 2. Nå som sync er ferdig, returnerer vi bare filene som ligger i cachen
        return cachePath.listFiles { _, name -> name.endsWith(".jpg") }?.toList() ?: emptyList()
    }

    override fun getFile(device: Device, storeFile: File, importFile: KFile): File? {
        log.info("Using $storeFile to store import of ${importFile.name} from ${importFile.device.id}")
        val out = gPhoto2.copyFile(device.path, importFile.path, importFile.name, storeFile) { progress ->
            log.info("Import progress on ${importFile.name} ($progress)")
        }
        return out
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