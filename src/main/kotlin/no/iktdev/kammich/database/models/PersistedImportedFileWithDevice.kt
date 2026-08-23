package no.iktdev.kammich.database.models

import no.iktdev.kammich.models.FileType
import no.iktdev.kammich.models.internal.PersistedDevice
import java.io.File
import java.util.UUID

data class PersistedImportedFileWithDevice(
    val id: Long,
    val importJob: UUID,
    val device: PersistedDevice, // Hele enheten inkludert serialnummer!
    val fileName: String,
    val fileType: FileType,
    val fileSize: Long,
    val extension: String,
    val checksum: String,
    val checksumType: String,
    val importedAt: String
) {
    fun getFile(mediaPath: String): File {
        return File(mediaPath, "${device.serialNumber}/$fileName")
    }
}