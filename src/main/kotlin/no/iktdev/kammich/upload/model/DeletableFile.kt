package no.iktdev.kammich.upload.model

import java.util.UUID

data class DeletableFile(
    val fileId: Long,
    val uploadId: Long,
    val assetId: UUID,
)
