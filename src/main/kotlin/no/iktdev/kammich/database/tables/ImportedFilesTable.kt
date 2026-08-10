package no.iktdev.kammich.database.tables

import no.iktdev.kammich.database.models.PersistedImportedFiles
import no.iktdev.kammich.models.FileType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.jdbc.selectAll

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
                    id = it[id].value.toLong(),
                    deviceId = it[deviceId].value,
                    fileName = it[fileName],
                    fileType = it[fileType],
                    fileSize = it[fileSize],
                    extension = it[extension],
                    checksum = it[checksum],
                    checksumType = it[checksumType],
                    importedAt = it[importedAt],
                )
            }
    }

    fun ResultRow.toPersisted(): PersistedImportedFiles {
        return PersistedImportedFiles(
            id = this[id].value.toLong(),
            deviceId = this[deviceId].value,
            fileName = this[fileName],
            fileType = this[fileType],
            fileSize = this[fileSize],
            extension = this[extension],
            checksum = this[checksum],
            checksumType = this[checksumType],
            importedAt = this[importedAt]
        )
    }
}


