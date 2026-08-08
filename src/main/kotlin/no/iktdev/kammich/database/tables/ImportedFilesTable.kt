package no.iktdev.kammich.database.tables

import no.iktdev.kammich.models.FileType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant

object ImportedFilesTable : IntIdTable("IMPORTED_FILES") {
    val deviceId = reference("DEVICE_ID", DevicesTable) // Refererer til DevicesTable
    val fileName = text("FILE_NAME")
    val fileType = enumerationByName("FILE_TYPE", 50, FileType::class)
    val fileSize = long("FILE_SIZE")
    val extension = text("EXTENSION")
    val checksum = varchar("CHECKSUM", 50)
    val checksumType = varchar("CHECKSUM_TYPE", 12)
    val importedAt = text("IMPORTED_AT")


    fun getWhere(predicate: () -> Op<Boolean>): List<PersistedImportedFiles> {
        return ImportedFilesTable.selectAll()
            .where(predicate) // Kan nå også skrives direkte som .where { ... }
            .orderBy(ImportedFilesTable.id, SortOrder.DESC)
            .map {
                PersistedImportedFiles(
                    id = it[ImportedFilesTable.id].value.toLong(),
                    deviceId = it[ImportedFilesTable.deviceId].value,
                    fileName = it[ImportedFilesTable.fileName],
                    fileType = it[ImportedFilesTable.fileType],
                    fileSize = it[ImportedFilesTable.fileSize],
                    extension = it[ImportedFilesTable.extension],
                    checksum = it[ImportedFilesTable.checksum],
                    checksumType = it[ImportedFilesTable.checksumType],
                    importedAt = it[ImportedFilesTable.importedAt],
                )
            }
    }

    fun ResultRow.toPersisted(): PersistedImportedFiles {
        return PersistedImportedFiles(
            id = this[ImportedFilesTable.id].value.toLong(),
            deviceId = this[ImportedFilesTable.deviceId].value,
            fileName = this[ImportedFilesTable.fileName],
            fileType = this[ImportedFilesTable.fileType],
            fileSize = this[ImportedFilesTable.fileSize],
            extension = this[ImportedFilesTable.extension],
            checksum = this[ImportedFilesTable.checksum],
            checksumType = this[ImportedFilesTable.checksumType],
            importedAt = this[ImportedFilesTable.importedAt]
        )
    }
}


