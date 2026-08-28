package no.iktdev.kammich.models.shared.update

data class AppUpdateProgress(
    val status: AppUpdateStatus,

    val version: String? = null,

    val progress: Int? = null,

    val message: String? = null,

    val error: String? = null
)