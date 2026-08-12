package no.iktdev.kammich.database.models

import no.iktdev.kammich.models.FileType
import java.util.UUID

data class PersistedImportedFile(
    val id: Long,
    val importJob: UUID,
    val deviceId: Long,
    val fileName: String,
    val fileType: FileType,
    val fileSize: Long,
    val extension: String,
    val checksum: String,
    val checksumType: String,
    val importedAt: String,
)