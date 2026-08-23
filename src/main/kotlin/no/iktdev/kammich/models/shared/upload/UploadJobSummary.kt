package no.iktdev.kammich.models.shared.upload

import java.util.UUID

data class UploadJobSummary(
    val userId: UUID,
    val jobId: UUID,
    val totalSuccess: Int,
    val totalFailure: Int,
    val total: Int
)