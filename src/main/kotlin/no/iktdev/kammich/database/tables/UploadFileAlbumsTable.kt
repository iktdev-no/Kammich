package no.iktdev.kammich.database.tables

import no.iktdev.kammich.models.shared.UploadState
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import java.time.Instant


object UploadFileAlbumsTable : LongIdTable("UPLOAD_FILE_ALBUMS") {
    val uploadFileId = reference("UPLOAD_FILE_ID", UploadFilesTable, onDelete = ReferenceOption.CASCADE)
    val albumId = reference("ALBUM_ID", AlbumsTable, onDelete = ReferenceOption.CASCADE)
    val updatedAt = text("UPDATED_AT").clientDefault { (Instant.now().toString()) }
    val state = enumerationByName<UploadState>("STATE", 20).default(UploadState.Pending)
    val errorMessage = text("ERROR_MESSAGE").nullable()

    init {
        uniqueIndex(uploadFileId, albumId)
    }
}