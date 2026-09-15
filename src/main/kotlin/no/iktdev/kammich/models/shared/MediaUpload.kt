package no.iktdev.kammich.models.shared

import java.util.UUID

data class UploadProgressEvent(
    val jobId: UUID,
    val totalFiles: Int,
    val successfulFiles: Int,
    val failedFiles: Int,
    val items: List<UploadMediaItem>,
    val state: UploadJobStatus // f.eks. RUNNING, COMPLETED, FAILED
)

data class UploadMediaItem(
    val fileName: String,
    val fileSize: Long,
    val upload: UploadState,
    val verification: Verification
)

enum class UploadState {
    Pending,
    Uploading,
    Success,
    Failure,
}


enum class UploadJobStatus {
    Queued,
    Running,
    Stopped,
    Completed,
    Failed,
    Cancelled
}

enum class Verification {
    Verified,
    Verifying,
    NotVerified,
    Failed
}