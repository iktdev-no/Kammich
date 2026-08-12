package no.iktdev.kammich.models.shared.device

data class DeviceOwnershipSummary(
    val deviceId: String,
    val name: String,
    val model: String?,
    val manufacturer: String?,
    val deviceType: DeviceType,
    val claimable: Boolean,
    val claimedBy: String?
)

data class ImportJobOwnershipSummary(
    val jobId: String,
    val deviceId: String,
    val totalFiles: Int,
    val claimable: Boolean,
    val claimedBy: String?
)