package no.iktdev.kammich.models.shared.storage

data class StorageStats(
    val totalBytes: Long,
    val freeBytes: Long,
    val usableBytes: Long, // Plass som faktisk kan skrives til
    val percentUsed: Double = if (totalBytes > 0) ((totalBytes - freeBytes).toDouble() / totalBytes) * 100 else 0.0,
    val isMounted: Boolean
)