package no.iktdev.kammich

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.google.gson.GsonBuilder
import net.jpountz.xxhash.XXHashFactory
import no.iktdev.kammich.database.tables.DeleteFilesTable
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
import no.iktdev.kammich.models.shared.NotificationKey
import no.iktdev.kammich.models.shared.NotificationMessageArgKey
import no.iktdev.kammich.models.shared.NotificationType
import no.iktdev.kammich.models.shared.Severity
import no.iktdev.kammich.models.shared.deletion.DeleteState
import no.iktdev.kammich.models.shared.device.Capability
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Query
import org.springframework.context.ApplicationEventPublisher
import java.io.File
import java.lang.Long
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

fun UUID.short(): String {
    return this.toString().substring(0, 8)
}

fun String.toMD5(): String {
    return this.toByteArray().let {
        MessageDigest.getInstance("MD5").digest(it)
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

    val errorKey = when {
        !created -> NotificationKey.SystemCreationFailureFolder
        !writable -> NotificationKey.SystemWriteFailureFolder
        true -> return this
        else -> NotificationKey.SystemUnknownFailureFolder
    }

    eventPublisher.publishEvent(
        Notification(
            id = notificationId,
            key = errorKey,
            messageArgs = mapOf(NotificationMessageArgKey.Path to this.absolutePath),
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
    key: NotificationKey,
    messageArgs: Map<NotificationMessageArgKey, String>,
    type: NotificationType = NotificationType.Alert
) {
    this.publishEvent(
        Notification(
            id = id,
            key = key,
            messageArgs = messageArgs,
            severity = Severity.Info,
            dismissable = true,
            type = type
        )
    )
}

fun ApplicationEventPublisher.warningNotification(
    id: String,
    key: NotificationKey,
    messageArgs: Map<NotificationMessageArgKey, String>,

    type: NotificationType = NotificationType.Alert
) {
    this.publishEvent(
        Notification(
            id = id,
            key = key,
            messageArgs = messageArgs,
            severity = Severity.Warning,
            dismissable = true,
            type = type
        )
    )
}

fun ApplicationEventPublisher.errorNotification(
    id: String,
    key: NotificationKey,
    messageArgs: Map<NotificationMessageArgKey, String>,
    type: NotificationType = NotificationType.Alert
) {
    this.publishEvent(
        Notification(
            id = id,
            key = key,
            messageArgs = messageArgs,
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
        hash = Long.toHexString(streamHasher.value),
        method = FileHashType.XX64Hash
    )
}

fun Query.whereLocalFilesOnly(): Query {
    return where {
        DeleteFilesTable.id.isNull() or
                (DeleteFilesTable.localState neq DeleteState.Deleted)
    }
}

fun localFileCondition(): Op<Boolean> =
    DeleteFilesTable.id.isNull() or
            (DeleteFilesTable.localState neq DeleteState.Deleted)

fun File.toSha1(): FileHash {
    val digest = MessageDigest.getInstance("SHA-1")

    this.inputStream().use { input ->
        val buffer = ByteArray(1024 * 1024) // 1 MB buffer
        var bytesRead: Int

        while (input.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }

    return FileHash(
        hash = digest.digest().joinToString("") { "%02x".format(it) },
        method = FileHashType.SHA1
    )
}

fun File.toSha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")

    inputStream().use { input ->
        val buffer = ByteArray(1024 * 1024)

        while (true) {
            val bytesRead = input.read(buffer)

            if (bytesRead == -1) break

            digest.update(buffer, 0, bytesRead)
        }
    }

    return digest.digest()
        .joinToString("") { "%02x".format(it) }
}

fun Instant.asOffsetDateTime(): OffsetDateTime = this.atZone(ZoneId.systemDefault()).toOffsetDateTime()

fun File.getExifTimestamp(): Instant? {
    return try {
        val metadata = ImageMetadataReader.readMetadata(this)
        val directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
        directory?.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)?.toInstant()
    } catch (e: Exception) {
        null
    }
}

fun Instant.between(start: Instant, end: Instant): Boolean {
    return this in start..end
}

fun Instant.within(start: Instant, end: Instant): Boolean {
    val zone = ZoneId.systemDefault()
    val date = atZone(zone).toLocalDate()
    val startDate = start.atZone(zone).toLocalDate()
    val endDate = end.atZone(zone).toLocalDate()

    return date >= startDate && date <= endDate
}