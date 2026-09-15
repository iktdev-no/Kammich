package no.iktdev.kammich.upload.model

import no.iktdev.kammich.models.shared.UploadJobStatus
import java.util.UUID

data class UploadJob(
    val userId: UUID,
    val jobId: UUID,
    val items: List<UploadJobItem>,
    var status: UploadJobStatus = UploadJobStatus.Queued,
) {}