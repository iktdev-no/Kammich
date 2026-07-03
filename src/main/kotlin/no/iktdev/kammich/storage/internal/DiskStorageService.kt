package no.iktdev.kammich.storage.internal

import no.iktdev.kammich.models.storage.Device
import no.iktdev.kammich.models.storage.StorageStats
import no.iktdev.kammich.storage.DeviceDiscoveryService
import org.springframework.stereotype.Service
import java.io.File

@Service
class DiskStorageService() {
    fun getStorageStats(device: Device): StorageStats {
        // Vi bruker mountPoint som vi forhåpentligvis har i Device-objektet
        val file = File(device.mountPoint)

        return StorageStats(
            totalBytes = file.totalSpace,
            freeBytes = file.freeSpace,
            usableBytes = file.usableSpace,
            // En disk er montert hvis stien eksisterer, er en mappe, og kan leses
            isMounted = file.exists() && file.isDirectory && file.canRead()
        )
    }

    fun getStagingDirectory(device: Device): File {
        // Sjekk at vi har et serienummer, ellers vil stien bli ødelagt
        val serial = device.serialNumber.ifBlank { "unknown" }
        val dir = File("/var/lib/kammich/staging/$serial")

        if (!dir.exists() && !dir.mkdirs()) {
            throw IllegalStateException("Kunne ikke opprette staging-mappe: ${dir.absolutePath}")
        }
        return dir
    }
}