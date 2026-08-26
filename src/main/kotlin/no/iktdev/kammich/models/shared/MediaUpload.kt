package no.iktdev.kammich.models.shared

import java.util.UUID

data class UploadProgressEvent(
    val jobId: UUID,
    val totalFiles: Int,
    val successfulFiles: Int,
    val failedFiles: Int,
    val items: List<UploadMediaItem>,
    val state: JobStatus // f.eks. RUNNING, COMPLETED, FAILED
)

data class UploadMediaItem(
    val fileName: String,
    val fileSize: Long,
    val state: UploadState,
)

enum class UploadState {
    Pending,
    Uploading,
    Success,
    Failure,
}


enum class JobStatus {
    Running, Completed, Failed
}

enum class Verification {
    Verified,
    NotVerified,
    Failed
}