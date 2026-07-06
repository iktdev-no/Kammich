package no.iktdev.kammich.storage.internal

import no.iktdev.kammich.models.shared.storage.BlockDevice
import no.iktdev.kammich.models.shared.storage.StorageStats
import org.springframework.stereotype.Service
import java.io.File

@Service
class DiskStorageService() {
    fun getStorageStats(blockDevice: BlockDevice): StorageStats {
        // Vi bruker mountPoint som vi forhåpentligvis har i Device-objektet
        val file = File(blockDevice.mountPoint)

        return StorageStats(
            totalBytes = file.totalSpace,
            freeBytes = file.freeSpace,
            usableBytes = file.usableSpace,
            // En disk er montert hvis stien eksisterer, er en mappe, og kan leses
            isMounted = file.exists() && file.isDirectory && file.canRead()
        )
    }

    fun getStagingDirectory(blockDevice: BlockDevice): File {
        // Sjekk at vi har et serienummer, ellers vil stien bli ødelagt
        val serial = blockDevice.serialNumber.ifBlank { "unknown" }
        val dir = File("/var/lib/kammich/staging/$serial")

        if (!dir.exists() && !dir.mkdirs()) {
            throw IllegalStateException("Kunne ikke opprette staging-mappe: ${dir.absolutePath}")
        }
        return dir
    }
}