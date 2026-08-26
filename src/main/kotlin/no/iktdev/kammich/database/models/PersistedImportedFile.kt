package no.iktdev.kammich.database.models

import no.iktdev.kammich.models.FileType
import java.io.File
import java.util.UUID

data class PersistedImportedFile(
    val id: Long,
    val importJob: UUID,
    val deviceId: Long,
    val fileName: String,
    val fileType: FileType,
    val cameraPath: String,
    val fileSize: Long,
    val extension: String,
    val checksum: String,
    val checksumType: String,
    val importedAt: String,
) {
    fun getFile(mediaPath: String, deviceSn: String): File {
        return File(mediaPath, "$deviceSn/$fileName")
    }
}