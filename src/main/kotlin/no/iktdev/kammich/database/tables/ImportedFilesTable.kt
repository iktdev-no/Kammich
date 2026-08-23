package no.iktdev.kammich.database.tables

import no.iktdev.kammich.database.models.PersistedImportedFile
import no.iktdev.kammich.database.models.PersistedImportedFileWithDevice
import no.iktdev.kammich.database.tables.DevicesTable.toPersistedDevice
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
            .orderBy(id, SortOrder.DESC)
            .map { it.toPersisted() }
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

    // --- NY FERDIG-JOIN FUNKSJON ---
    fun getAllWithDevice(predicate: (() -> Op<Boolean>)? = null): List<PersistedImportedFileWithDevice> {
        val query = ImportedFilesTable.innerJoin(DevicesTable)
            .selectAll()

        if (predicate != null) {
            query.where(predicate)
        }

        return query
            .orderBy(id, SortOrder.DESC)
            .map { it.toPersistedWithDevice() }
    }

    // Mapper som leser ut både fil og enhet fra join-resultatet automatisk
    fun ResultRow.toPersistedWithDevice(): PersistedImportedFileWithDevice {
        return PersistedImportedFileWithDevice(
            id = this[id].value,
            importJob = UUID.fromString(this[importJob]),
            device = this.toPersistedDevice(), // Bruker DevicesTable sin mapper direkte på raden!
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


