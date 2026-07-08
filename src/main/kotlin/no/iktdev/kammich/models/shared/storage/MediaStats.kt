package no.iktdev.kammich.models.shared.storage

data class MediaStats(
    val manufacturer: String? = null,
    val model: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val percentUsed: Double,
    val serial: String,
    val transport: String,
    val photoCount: Long,
    val videoCount: Long
)