package no.iktdev.kammich.database.models

import no.iktdev.kammich.database.tables.UploadFilesTable
import no.iktdev.kammich.models.shared.UploadState
import no.iktdev.kammich.models.shared.Verification
import java.time.Instant
import java.util.UUID

data class PersistedUploadFile(
    val id: Long,
    val uploadJobId: UUID?,
    val importedFileId: Long,
    val immichUserId: UUID,
    val immichAssetId: UUID? = null,
    val state: UploadState,
    val retryCount: Int,
    val errorMessage: String? = null,
    val updatedAt: Instant,
    val verified: Verification
) {
}