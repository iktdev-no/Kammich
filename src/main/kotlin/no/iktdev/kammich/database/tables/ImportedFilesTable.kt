package no.iktdev.kammich.database.tables

import no.iktdev.kammich.models.FileType
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object ImportedFilesTable : IntIdTable("IMPORTED_FILES") {
    val deviceId = reference("DEVICE_ID", DevicesTable) // Refererer til DevicesTable
    val fileName = text("FILE_NAME")
    val fileType = enumerationByName("FILE_TYPE", 50, FileType::class)
    val fileSize = long("FILE_SIZE")
    val extension = text("EXTENSION")
    val checksum = varchar("CHECKSUM", 50)
    val checksumType = varchar("CHECKSUM_TYPE", 12)
    val importedAt = text("IMPORTED_AT")
}

