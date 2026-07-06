package no.iktdev.kammich.storage

import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.models.NotificationDismissed
import no.iktdev.kammich.models.shared.Notification
import no.iktdev.kammich.models.shared.NotificationType
import no.iktdev.kammich.models.shared.Severity
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.io.File

@Service
class DiskCacheService(
    private val config: ConfigService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(DiskCacheService::class.java)

    fun getCacheDirectory(deviceId: String): File {
        val rootPath = config.getConfig().cachePath
        val rootDir = File(rootPath)
        log.info("Using ${rootDir.absolutePath}")
        // Sjekk om root-cachen eksisterer og er skrivbar
        if (!rootDir.exists() && !rootDir.mkdirs()) {
            eventPublisher.publishEvent(Notification(
                id = "DiskCacheService-root-create",
                title = "Could not create directory",
                message = "Could not create directory: ${rootDir.absolutePath}",
                severity = Severity.Error,
                dismissable = false,
                type = NotificationType.Alert
            ))
            throw IllegalStateException("Kunne ikke opprette rot-cache: $rootPath")
        } else {
            eventPublisher.publishEvent(NotificationDismissed("DiskCacheService-root-create"))
        }
        if (!rootDir.canWrite()) {
            eventPublisher.publishEvent(Notification(
                id = "DiskCacheService-root-writable",
                title = "Permission denied",
                message = "Can't write to the directory: ${rootDir.absolutePath}",
                severity = Severity.Error,
                dismissable = false,
                type = NotificationType.Alert
            ))
            throw IllegalStateException("Har ikke skrivetilgang til cache-rot: $rootPath")
        } else {
            eventPublisher.publishEvent(NotificationDismissed("DiskCacheService-root-writable"))
        }

        val dir = File(rootDir, deviceId)
        if (!dir.exists()) {
            log.info("Prøver å opprette cache-mappe: ${dir.absolutePath}")
            if (!dir.mkdirs()) {
                // Her kan vi feilsøke mer:
                val existsButNotDir = dir.exists() && !dir.isDirectory

                throw IllegalStateException("Kunne ikke opprette ${dir.absolutePath}. (Finnes fil med samme navn? $existsButNotDir)")
            }
        }

        return dir
    }
}