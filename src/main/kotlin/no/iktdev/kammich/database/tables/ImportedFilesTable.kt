package no.iktdev.kammich.database.tables

import no.iktdev.kammich.database.models.PersistedImportedFile
import no.iktdev.kammich.models.FileType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID

object ImportedFilesTable : LongIdTable("IMPORTED_FILES") {
    val importJob = varchar("IMPORT_JOB", 36)
    val deviceId = reference("DEVICE_ID", DevicesTable) // Refererer til DevicesTable
    val fileName = text("FILE_NAME")
    val fileType = enumerationByName("FILE_TYPE", 50, FileType::class)
    val fileSize = long("FILE_SIZE")
    val extension = text("EXTENSION")
    val checksum = varchar("CHECKSUM", 50)
    val checksumType = varchar("CHECKSUM_TYPE", 12)
    val importedAt = text("IMPORTED_AT")


    fun getWhere(predicate: () -> Op<Boolean>): List<PersistedImportedFile> {
        return ImportedFilesTable.selectAll()
            .where(predicate) // Kan nå også skrives direkte som .where { ... }
            .orderBy(ImportedFilesTable.id, SortOrder.DESC)
            .map {
                PersistedImportedFile(
                    id = it[id].value,
                    importJob = it[importJob].let { x -> UUID.fromString(x) },
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

    fun ResultRow.toPersisted(): PersistedImportedFile {
        return PersistedImportedFile(
            id = this[id].value,
            deviceId = this[deviceId].value,
            importJob = this[importJob].let { x -> UUID.fromString(x) },
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


