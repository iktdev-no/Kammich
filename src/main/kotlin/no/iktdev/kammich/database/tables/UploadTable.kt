package no.iktdev.kammich.database.tables

import no.iktdev.kammich.database.models.PersistedUploadFile
import no.iktdev.kammich.models.shared.UploadState
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant
import java.util.UUID

object UploadFilesTable : LongIdTable("UPLOAD_FILES") {
    val importedFileId = reference("IMPORTED_FILE_ID", ImportedFilesTable, onDelete = ReferenceOption.CASCADE)
    val uploadJobId = varchar("UPLOAD_JOB", 36).nullable().default(null)
    val immichUserId = varchar("IMMICH_USER_ID", 36)

    val immichAssetId = varchar("IMMICH_ASSET_ID", 36).nullable()

    val state = enumerationByName<UploadState>("STATE", 50).default(UploadState.Pending)
    val retryCount = integer("RETRY_COUNT").default(0)
    val errorMessage = text("ERROR_MESSAGE").nullable()
    val updatedAt = text("UPDATED_AT").clientDefault { (Instant.now().toString()) }

    fun getWhere(predicate: () -> Op<Boolean>): List<PersistedUploadFile> {
        return UploadFilesTable.selectAll()
            .where(predicate)
            .map { it ->  it.toPersistedUploadFile()}
    }

    fun ResultRow.toPersistedUploadFile(): PersistedUploadFile {
        return PersistedUploadFile(
            id = this[id].value,
            uploadJobId = this[uploadJobId]?.let { UUID.fromString(it) },
            importedFileId = this[importedFileId].value,
            immichUserId = this[immichUserId].let { UUID.fromString(it) },
            immichAssetId = this[immichAssetId]?.let { UUID.fromString(it) },
            state = this[state],
            retryCount = this[retryCount],
            errorMessage = this[errorMessage],
            updatedAt = this[updatedAt].let { Instant.parse(it) }
        )
    }
}
