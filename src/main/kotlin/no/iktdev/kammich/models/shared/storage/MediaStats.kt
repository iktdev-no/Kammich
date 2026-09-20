package no.iktdev.kammich.models.shared.storage

data class MediaStats(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val percentUsed: Double,
    val transport: String,
    val photoCount: Long,
    val videoCount: Long
)