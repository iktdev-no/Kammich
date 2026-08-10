package no.iktdev.kammich.database.models

import no.iktdev.kammich.models.FileType

data class PersistedImportedFiles(
    val id: Long,
    val deviceId: Int,
    val fileName: String,
    val fileType: FileType,
    val fileSize: Long,
    val extension: String,
    val checksum: String,
    val checksumType: String,
    val importedAt: String,
)