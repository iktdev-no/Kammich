package no.iktdev.kammich.models.shared

import no.iktdev.ts.MapStrategy
import no.iktdev.ts.anno.TsMapStrategy

data class Notification(
    val id: String,
    val type: NotificationType,
    val key: NotificationKey,
    @TsMapStrategy(MapStrategy.MapAsPartialRecord)
    val messageArgs: Map<NotificationMessageArgKey, String> = emptyMap(),
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

enum class NotificationKey {
    ImportCompleted,
    ImportFailed,
    ImportNoNewFiles,
    ImportDeviceNotFullyAdded,
    CameraDisconnected,
    CameraCleanupCompleted,
    CameraCleanupFailed,
    CameraConnected,
    CameraCleanupNoFiles,
    CameraCleanupDisconnected,
    CameraDCIMMissing,
    SystemCreationFailureFolder,
    SystemWriteFailureFolder,
    SystemUnknownFailureFolder,
    ImportJobClaimFailed
}

enum class NotificationMessageArgKey {
    Path,
    SerialNumber,
    DeviceName,
    ModelManufacturer,
    TotalCount,
    FailedCount,
    DeletedCount,
    RemainingCount,
    ImportedCount,
    ImportFailedCount,
    JobId,
    ExceptionMessageRaw
}