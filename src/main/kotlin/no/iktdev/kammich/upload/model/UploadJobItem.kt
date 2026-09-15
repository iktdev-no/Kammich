package no.iktdev.kammich.upload.model

import no.iktdev.kammich.models.shared.UploadState
import no.iktdev.kammich.models.shared.Verification


data class UploadJobItem(
    val uploadId: Long,
    val fileId: Long,
    var upload: UploadState = UploadState.Pending,
    var verify: Verification = Verification.NotVerified
)