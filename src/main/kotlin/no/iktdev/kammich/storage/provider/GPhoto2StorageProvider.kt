package no.iktdev.kammich.storage.provider

import no.iktdev.kammich.gphoto2.IGPhoto2
import no.iktdev.kammich.gphoto2.model.GPhoto2File
import no.iktdev.kammich.gphoto2.model.GPhoto2NodeType
import no.iktdev.kammich.models.internal.KFile
import no.iktdev.kammich.models.internal.KFileType
import no.iktdev.kammich.models.shared.device.GPhoto2Device
import no.iktdev.kammich.models.shared.device.RemovableDevice
import no.iktdev.kammich.toMD5
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class GPhoto2StorageProvider(
    private val gPhoto2: IGPhoto2
): StorageProvider {
    private val log = LoggerFactory.getLogger(GPhoto2StorageProvider::class.java)

    override fun listFiles(device: RemovableDevice, path: String?): List<KFile> {
        val g = device as GPhoto2Device
        val files = gPhoto2.getFiles(g.port, path ?: "")
        return files.map { it.toKFile() }
    }

    override fun listAllFiles(device: RemovableDevice, path: String?): List<KFile> {
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

    override fun getDCIM(device: RemovableDevice): KFile? {
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

    override fun copyFile(device: RemovableDevice, storeFile: File, importFile: KFile): File? {
        val g = device as GPhoto2Device
        log.info("Using $storeFile to store import of ${importFile.name} from ${g.id}")
        val out = gPhoto2.copyFile(g.port, importFile.path, importFile.name, storeFile) { progress ->
            log.info("Import progress on ${importFile.name} ($progress)")
        }
        return out
    }

    fun GPhoto2File.toKFile(): KFile {
        return KFile(
            id = "$folderPath/$name".toMD5(),
            name = name,
            path = folderPath,
            size = sizeBytes,
            type = when (type) { GPhoto2NodeType.FILE -> {
                KFileType.FILE
            } GPhoto2NodeType.FOLDER -> {
                KFileType.DIRECTORY
            } }
        )
    }
}