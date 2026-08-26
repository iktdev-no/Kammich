package no.iktdev.kammich.database.tables

import no.iktdev.kammich.database.models.PersistedDeleteFile
import no.iktdev.kammich.models.shared.deletion.DeleteState
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant

object DeleteFilesTable : LongIdTable("DELETE_FILES") {
    val importedFileId = reference("IMPORTED_FILE_ID", ImportedFilesTable, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val uploadFileId = reference("UPLOAD_FILE_ID", UploadFilesTable, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val cameraState = enumerationByName<DeleteState>("CAMERA_STATE", 50).default(DeleteState.Pending).clientDefault { DeleteState.Pending }
    val cameraDeletedAt = text("CAMERA_DELETED_AT").nullable()
    val cameraErrorMessage = text("CAMERA_ERROR_MESSAGE").nullable()
    val localState = enumerationByName<DeleteState>("LOCAL_STATE", 50).default(DeleteState.Pending).clientDefault { DeleteState.Pending }
    val localDeletedAt = text("LOCAL_DELETED_AT").nullable()
    val localErrorMessage = text("LOCAL_ERROR_MESSAGE").nullable()
    val retryCount = integer("RETRY_COUNT").default(0)
    val createdAt = text("CREATED_AT").clientDefault { Instant.now().toString() }
    val updatedAt = text("UPDATED_AT").clientDefault { Instant.now().toString() }


    fun getLocallyDeletedIds(): List<Long> {
        return DeleteFilesTable
            .select(DeleteFilesTable.importedFileId)
            .where {
                DeleteFilesTable.localState eq DeleteState.Deleted
            }
            .map { it[DeleteFilesTable.importedFileId].value }
    }

    fun getWhere(predicate: () -> Op<Boolean>): List<PersistedDeleteFile> {
        return DeleteFilesTable
            .selectAll()
            .where(predicate)
            .map { it.toPersisted() }
    }

    fun ResultRow.toPersisted(): PersistedDeleteFile {
        return PersistedDeleteFile(
            id = this[id].value,
            importedFileId = this[importedFileId].value,
            uploadFileId = this[uploadFileId].value,

            cameraState = this[cameraState],
            cameraDeletedAt = this[cameraDeletedAt]?.let(Instant::parse),
            cameraErrorMessage = this[cameraErrorMessage],

            localState = this[localState],
            localDeletedAt = this[localDeletedAt]?.let(Instant::parse),
            localErrorMessage = this[localErrorMessage],

            retryCount = this[retryCount],

            createdAt = Instant.parse(this[createdAt]),
            updatedAt = Instant.parse(this[updatedAt])
        )
    }
}