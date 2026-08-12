package no.iktdev.kammich.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object UploadFilesTable : LongIdTable("UPLOAD_FILES") {
    val importedFileId = reference("IMPORTED_FILE_ID", ImportedFilesTable, onDelete = ReferenceOption.CASCADE)
    val immichUserId = varchar("IMMICH_USER_ID", 36)

    val albumId = optReference("ALBUM_ID", AlbumsTable, onDelete = ReferenceOption.SET_NULL)

    // Lagrer enum-verdien som tekst i databasen ("NOT_READY", "READY", etc.)
    val state = enumerationByName<UploadState>("STATE", 50).default(UploadState.NOT_READY)
    val retryCount = integer("RETRY_COUNT").default(0)
    val errorMessage = text("ERROR_MESSAGE").nullable()
    val updatedAt = text("UPDATED_AT")
}

enum class UploadState {
    NOT_READY,
    READY,
    IN_QUEUE,
    UPLOADED,
    FAILED
}