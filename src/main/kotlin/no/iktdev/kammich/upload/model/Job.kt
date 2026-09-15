package no.iktdev.kammich.upload.model

sealed class UploadJobResult(open val job: UploadJob)
data class UploadJobSuccess(override val job: UploadJob): UploadJobResult(job)
data class UploadJobFailed(override val job: UploadJob, val reason: String): UploadJobResult(job)