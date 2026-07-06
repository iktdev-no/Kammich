package no.iktdev.kammich.models.shared

data class Notification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val severity: Severity,
    val dismissed: Boolean = false,
    val dismissable: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class NotificationType {
    Alert,
}

enum class Severity {
    Info,
    Warning,
    Error,
}