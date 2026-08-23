package no.iktdev.kammich.models.internal.immich

import java.io.File
import java.time.Instant

data class UploadAssetRequest(
    val file: File,
    val createdAt: Instant,
    val modifiedAt: Instant,
)