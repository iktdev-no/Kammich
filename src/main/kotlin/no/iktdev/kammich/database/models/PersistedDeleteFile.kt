package no.iktdev.kammich.database.models

import no.iktdev.kammich.models.shared.deletion.DeleteState
import java.time.Instant

data class PersistedDeleteFile(
    val id: Long,
    val importedFileId: Long,
    val uploadFileId: Long,

    val cameraState: DeleteState,
    val cameraDeletedAt: Instant?,
    val cameraErrorMessage: String?,

    val localState: DeleteState,
    val localDeletedAt: Instant?,
    val localErrorMessage: String?,

    val retryCount: Int,

    val createdAt: Instant,
    val updatedAt: Instant
)