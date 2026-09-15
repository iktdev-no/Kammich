package no.iktdev.kammich.upload.model

import java.util.UUID

sealed class UploadResult()
data class UploadSuccess(val assetId: UUID): UploadResult()
data class UploadFailed(val reason: String): UploadResult()


sealed class VerificationResult()
data class VerifiedSuccess(val uploadId: Long): VerificationResult()
data class VerificationSkipped(val uploadId: Long): VerificationResult()
data class VerificationFailed(val uploadId: Long, val reason: String): VerificationResult()