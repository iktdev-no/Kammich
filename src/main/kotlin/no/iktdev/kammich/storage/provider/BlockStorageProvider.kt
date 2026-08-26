package no.iktdev.kammich.storage.provider

import no.iktdev.kammich.models.internal.KFile
import no.iktdev.kammich.models.internal.KFileType
import no.iktdev.kammich.models.shared.device.BlockDevice
import no.iktdev.kammich.models.shared.device.RemovableDevice
import no.iktdev.kammich.toMD5
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class BlockStorageProvider: StorageProvider {
    private val log = LoggerFactory.getLogger(BlockStorageProvider::class.java)

    override fun listFiles(device: RemovableDevice, path: String?): List<KFile> {
        val root = (device as BlockDevice).mountPoint?.let { File(it) } ?: return emptyList()
        val targetDir = File(root, path ?: "")
        if (!targetDir.exists() || !targetDir.isDirectory) return emptyList()

        return targetDir.listFiles()?.map { it.toKFile(device) } ?: emptyList()
    }

    override fun listAllFiles(device: RemovableDevice, path: String?): List<KFile> {
        val root = (device as BlockDevice).mountPoint?.let { File(it) } ?: return emptyList()

        val targetDir = File(root, path ?: "")
        if (!targetDir.exists() || !targetDir.isDirectory) return emptyList()

        // Bruker walkTopDown for å hente alt rekursivt
        return targetDir.walkTopDown()
            .filter { it.isFile }
            .map { it.toKFile(device) }
            .toList()
    }

    override fun getDCIM(device: RemovableDevice): KFile? {
        val root = (device as BlockDevice).mountPoint?.let { File(it) } ?: return null
        // Fujifilm og andre kameraer legger ofte DCIM i roten eller en /DCIM-mappe
        // Vi leter etter en mappe som heter "DCIM" (case-insensitive)
        val dcim = root.listFiles()?.find {
            it.isDirectory && it.name.equals("DCIM", ignoreCase = true)
        }

        return dcim?.toKFile(device)
    }


    override fun copyFile(
        device: RemovableDevice,
        storeFile: File,
        importFile: KFile
    ): File? {
        val blockDevice = device as BlockDevice
        val root = blockDevice.mountPoint?.let { File(it) } ?: return null
        val source = File(root, importFile.path.removePrefix("/"))

        if (!source.exists() || !source.isFile) {
            log.warn("Could not find file {} on device {}", importFile.path, device.id)
            return null
        }

        return try {
            val target = File(storeFile, importFile.name)
            source.copyTo(target, overwrite = false)
            target
        } catch (e: Exception) {
            log.error("Failed to copy {} from device {}", importFile.path, device.id, e)
            null
        }
    }

    override fun deleteFile(
        device: RemovableDevice,
        file: KFile
    ): Boolean {
        val blockDevice = device as BlockDevice
        val root = blockDevice.mountPoint?.let { File(it) } ?: return false
        val target = File(root, file.path.removePrefix("/"))

        if (!target.exists()) {
            log.debug("File {} is already gone from device {}", file.path, device.id)
            return true
        }

        return try {
            target.delete()
        } catch (e: Exception) {
            log.error("Failed to delete {} from device {}", file.path, device.id, e)
            false
        }
    }

    private fun File.toKFile(device: BlockDevice): KFile {
        val root = File(device.mountPoint!!)

        // Vi henter relativ sti, fjerner "." hvis den er rot, og legger på ledende slash
        val rel = this.relativeTo(root).path
        val normalizedPath = if (rel == ".") "/" else "/$rel"

        return KFile(
            id = (device.id + ":" + normalizedPath).toMD5(),
            path = normalizedPath,
            name = if (this == root) root.name else this.name,
            size = this.length(),
            type = if (this.isDirectory) KFileType.DIRECTORY else KFileType.FILE,
        )
    }
}