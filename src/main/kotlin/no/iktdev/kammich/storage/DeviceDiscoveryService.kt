package no.iktdev.kammich.storage

import no.iktdev.kammich.models.storage.internal.DiskInfo
import org.springframework.stereotype.Service

@Service
class DeviceDiscoveryService {

    // Returnerer disker sortert etter transport-type
    fun getAvailableDisks(includeUsb: Boolean = false): List<DiskInfo> {
        // Vi ber om NAME, TYPE og TRAN (transport-type)
        val result = ProcessBuilder("lsblk", "-d", "-n", "-o", "NAME,TYPE,TRAN")
            .start().inputStream.bufferedReader().readText()

        return result.lines()
            .filter { it.contains("disk") }
            .map { line ->
                val parts = line.split(Regex("\\s+"))
                DiskInfo(
                    path = "/dev/${parts[0]}",
                    type = parts[1],
                    transport = parts.getOrNull(2) ?: "unknown"
                )
            }
            .filter { disk ->
                // Hvis includeUsb er false, filtrer bort alt som har 'usb' som transport
                if (!includeUsb) disk.transport != "usb" else true
            }
    }
}