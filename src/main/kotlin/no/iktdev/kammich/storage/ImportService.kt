package no.iktdev.kammich.storage

import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.getDeviceId
import no.iktdev.kammich.ensureWritable
import no.iktdev.kammich.models.shared.Notification
import no.iktdev.kammich.models.shared.NotificationType
import no.iktdev.kammich.models.shared.Severity
import no.iktdev.kammich.models.shared.files.KFile
import no.iktdev.kammich.models.shared.storage.removable.Device
import no.iktdev.kammich.repository.FileRepository
import no.iktdev.kammich.storage.provider.StorageProviderFactory
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.io.File
import java.time.ZonedDateTime

@Service
class ImportService(
    private val config: ConfigService,
    private val fileRepository: FileRepository,
    private val providerFactory: StorageProviderFactory,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(ImportService::class.java)


    fun importFiles(device: Device, files: List<KFile>): List<File> {
        val provider = providerFactory.getProvider(device)
        log.info("Config: ${config.getConfig()}")
        val mediaRoot = File(config.getConfig().mediaPath).ensureWritable(
            eventPublisher, "ImportService-mediaRoot"
        ) ?: return emptyList()

        val storage = File(mediaRoot, device.id).ensureWritable(
            eventPublisher, "ImportService-deviceFolder-${device.id}"
        ) ?: return emptyList()

        val deviceId = DevicesTable.getDeviceId(device.id) ?: run {
            eventPublisher.publishEvent(Notification(
                id = "ImportService-Device-not-present",
                title = "Device ${device.id} missing",
                message = "Device ${device.id} is not stored in database!\nCan't import files untill this is resolved",
                severity = Severity.Warning,
                dismissable = false,
                type = NotificationType.Alert
            ))
            return emptyList()
        }

        val importedFiles = files.mapNotNull { file ->
            val imported = provider.getFile(device, storage, file)
            if (imported != null) {
                imported to ZonedDateTime.now()
            } else null
        }

        fileRepository.saveFiles(deviceId, importedFiles)
        log.info("Imported ${importedFiles.size} device ${device.id}")

        return importedFiles.map { it.first }
    }



}