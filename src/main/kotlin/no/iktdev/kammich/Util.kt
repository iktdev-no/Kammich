package no.iktdev.kammich

import no.iktdev.kammich.models.FileType
import no.iktdev.kammich.models.FileType.IMAGE
import no.iktdev.kammich.models.FileType.OTHER
import no.iktdev.kammich.models.FileType.VIDEO
import no.iktdev.kammich.models.NotificationDismissed
import no.iktdev.kammich.models.shared.Notification
import no.iktdev.kammich.models.shared.NotificationType
import no.iktdev.kammich.models.shared.Severity
import org.springframework.context.ApplicationEventPublisher
import java.io.File

fun String.toMD5(): String {
    return this.toByteArray().let {
        java.security.MessageDigest.getInstance("MD5").digest(it)
            .joinToString("") { b -> "%02x".format(b) }
    }
}

fun File.ensureWritable(eventPublisher: ApplicationEventPublisher, notificationId: String): File? {
    if (this.exists() && this.canWrite()) {
        eventPublisher.publishEvent(NotificationDismissed(notificationId))
        return this
    }

    val created = if (!this.exists()) this.mkdirs() else true
    val writable = this.canWrite()

    System.out.println("Created $created writable: $writable")


    val errorMessage = when {
        !created -> Pair("Unable to create", "Unable to create folder at ${this.absolutePath}")
        !writable -> Pair("Unusable folder", "Unable to write to folder at ${this.absolutePath}")
        true -> return this
        else -> Pair("Unknown failure", "Unable to work at or with folder at ${this.absolutePath}")
    }

    eventPublisher.publishEvent(
        Notification(
            id = notificationId,
            title = errorMessage.first,
            message = errorMessage.second,
            severity = Severity.Error,
            dismissable = false,
            type = NotificationType.Alert
        )
    )
    return null
}

fun File.getFileType(): FileType {
    return when (this.extension.lowercase()) {
        "jpg", "jpeg", "png", "webp", "heic" -> IMAGE
        "mp4", "mov", "avi", "mkv" -> VIDEO
        else -> OTHER
    }
}
