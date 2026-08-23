package no.iktdev.kammich.models.internal.events

import java.util.UUID

data class UploadCompletedEvent(val userId: UUID, val assets: List<UploadedAssets>)

data class UploadedAssets(val uploadedId: Long, val assetId: UUID, val absolutePath: String)
