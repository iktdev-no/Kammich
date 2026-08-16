package no.iktdev.kammich.models.shared.immich.api

data class ImmichServerStorage(
    val diskAvailable: String,
    val diskAvailableRaw: Long,
    val diskSize: String,
    val diskSizeRaw: Long,
    val diskUsagePercentage: Double,
    val diskUse: String,
    val diskUseRaw: Long
)