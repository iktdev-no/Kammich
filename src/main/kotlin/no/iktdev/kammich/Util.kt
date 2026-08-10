package no.iktdev.kammich

import com.google.gson.GsonBuilder
import net.jpountz.xxhash.XXHashFactory
import no.iktdev.kammich.gphoto2.model.GPhoto2DeviceAbility
import no.iktdev.kammich.gphoto2.model.GPhoto2DeviceInfo
import no.iktdev.kammich.gphoto2.parsers.GPhoto2AbilityParser
import no.iktdev.kammich.models.FileHash
import no.iktdev.kammich.models.FileHashType
import no.iktdev.kammich.models.FileType
import no.iktdev.kammich.models.FileType.IMAGE
import no.iktdev.kammich.models.FileType.OTHER
import no.iktdev.kammich.models.FileType.VIDEO
import no.iktdev.kammich.models.NotificationDismissed
import no.iktdev.kammich.models.shared.Notification
import no.iktdev.kammich.models.shared.NotificationType
import no.iktdev.kammich.models.shared.Severity
import no.iktdev.kammich.models.shared.device.Capability
import org.springframework.context.ApplicationEventPublisher
import java.io.File
import java.time.Instant
import java.time.LocalDateTime

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

fun ApplicationEventPublisher.infoNotification(
    id: String,
    title: String,
    message: String,
    type: NotificationType = NotificationType.Alert
) {
    this.publishEvent(
        Notification(
            id = id,
            title = title,
            message = message,
            severity = Severity.Info,
            dismissable = true,
            type = type
        )
    )
}

fun ApplicationEventPublisher.warningNotification(
    id: String,
    title: String,
    message: String,
    type: NotificationType = NotificationType.Alert
) {
    this.publishEvent(
        Notification(
            id = id,
            title = title,
            message = message,
            severity = Severity.Warning,
            dismissable = true,
            type = type
        )
    )
}

fun ApplicationEventPublisher.errorNotification(
    id: String,
    title: String,
    message: String,
    type: NotificationType = NotificationType.Alert
) {
    this.publishEvent(
        Notification(
            id = id,
            title = title,
            message = message,
            severity = Severity.Error,
            dismissable = true,
            type = type
        )
    )
}


fun GPhoto2DeviceAbility.toCaps(): List<Capability> {
    return listOfNotNull(
        Capability.CAPTURE.takeIf { this.captureChoices.isNotEmpty() },
        Capability.DELETE.takeIf { this.deleteSelectedFiles || this.deleteAllFiles },
        Capability.UPLOAD.takeIf { this.fileUploadSupport },
        Capability.PREVIEW.takeIf { this.filePreviewSupport },
        Capability.CONFIGURE.takeIf { this.configurationSupport }
    )
}

fun File.toXxHash(): FileHash {
    val factory = XXHashFactory.nativeInstance()
    val streamHasher = factory.newStreamingHash64(0) // 0 som seed

    this.inputStream().use { input ->
        val buffer = ByteArray(1024 * 1024) // 1 MB buffer
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            streamHasher.update(buffer, 0, bytesRead)
        }
    }

    return FileHash(
        hash = java.lang.Long.toHexString(streamHasher.value),
        method = FileHashType.XX64Hash
    )
}