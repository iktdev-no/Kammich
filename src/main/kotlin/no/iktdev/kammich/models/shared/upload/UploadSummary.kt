package no.iktdev.kammich.models.shared.upload

import java.time.Instant
import java.util.UUID

data class UploadSummary(
    val userId: UUID,
    val totalUploads: Long,
    val totalReadyUploads: Long,
    val totalInQueueUploads: Long,
    val totalFailedUploads: Long,
    val totalSucceededUploads: Long,
    val lastUpdatedAt: Instant?,
)