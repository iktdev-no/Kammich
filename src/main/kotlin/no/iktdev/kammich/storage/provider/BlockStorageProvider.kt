package no.iktdev.kammich.storage.provider

import no.iktdev.kammich.models.shared.files.KFile
import no.iktdev.kammich.models.shared.files.KFileType
import no.iktdev.kammich.models.shared.storage.removable.Device
import no.iktdev.kammich.storage.DeviceMonitorService
import no.iktdev.kammich.toMD5
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import kotlin.math.log

@Service
class BlockStorageProvider: StorageProvider {
    private val log = LoggerFactory.getLogger(BlockStorageProvider::class.java)

    override fun listFiles(device: Device, path: String?): List<KFile> {
        val targetDir = File(device.path, path ?: "")
        if (!targetDir.exists() || !targetDir.isDirectory) return emptyList()

        return targetDir.listFiles()?.map { it.toKFile(device) } ?: emptyList()
    }

    override fun listAllFiles(device: Device, path: String?): List<KFile> {
        val targetDir = File(device.path, path ?: "")
        if (!targetDir.exists() || !targetDir.isDirectory) return emptyList()

        // Bruker walkTopDown for å hente alt rekursivt
        return targetDir.walkTopDown()
            .filter { it.isFile }
            .map { it.toKFile(device) }
            .toList()
    }

    override fun getDCIM(device: Device): KFile? {
        val root = File(device.path)
        // Fujifilm og andre kameraer legger ofte DCIM i roten eller en /DCIM-mappe
        // Vi leter etter en mappe som heter "DCIM" (case-insensitive)
        val dcim = root.listFiles()?.find {
            it.isDirectory && it.name.equals("DCIM", ignoreCase = true)
        }

        return dcim?.toKFile(device)
    }


    override fun copyFile(
        device: Device,
        storeFile: File,
        importFile: KFile
    ): File? {
        TODO("Not yet implemented")
    }

    private fun File.toKFile(device: Device): KFile {
        val root = File(device.path)

        // Vi henter relativ sti, fjerner "." hvis den er rot, og legger på ledende slash
        val rel = this.relativeTo(root).path
        val normalizedPath = if (rel == ".") "/" else "/$rel"

        return KFile(
            id = (device.id + ":" + normalizedPath).toMD5(),
            path = normalizedPath,
            name = if (this == root) root.name else this.name,
            size = this.length(),
            type = if (this.isDirectory) KFileType.DIRECTORY else KFileType.FILE,
            device = device
        )
    }
}