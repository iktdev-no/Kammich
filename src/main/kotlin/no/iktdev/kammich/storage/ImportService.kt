package no.iktdev.kammich.storage

import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.getDeviceId
import no.iktdev.kammich.ensureWritable
import no.iktdev.kammich.infoNotification
import no.iktdev.kammich.models.shared.files.KFile
import no.iktdev.kammich.models.shared.storage.removable.Device
import no.iktdev.kammich.repository.FileRepository
import no.iktdev.kammich.storage.provider.StorageProviderFactory
import no.iktdev.kammich.warningNotification
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
            eventPublisher.warningNotification("ImportService-Device-not-present-${device.id}",
                "Device ${device.id} missing",
                "Device ${device.id} is not stored in database!\nCan't import files untill this is resolved",
            )
            return emptyList()
        }

        if (files.isEmpty()) {
            log.info("No files to import")
        }

        val importedFiles = files.mapNotNull { file ->
            val imported = provider.copyFile(device, storage, file)
            if (imported != null) {
                imported to ZonedDateTime.now()
            } else null
        }

        val success = if (importedFiles.isNotEmpty()) {
            fileRepository.saveFiles(deviceId, importedFiles)
        } else true

        if (success) {
            if (importedFiles.isEmpty()) {
                eventPublisher.infoNotification("ImportService-NoFiles-${device.id}", "No files to import", "All files have already been imported for ${device.model ?: device.id}")
            } else {
                eventPublisher.infoNotification("ImportService-Success-${device.id}", "Imported ${importedFiles.size} files", "Imported ${importedFiles.size} files from device ${device.model ?: device.id}")
            }
            log.info("Imported ${importedFiles.size} device ${device.id}")
        } else {
            log.info("Imported ${importedFiles.size} device ${device.id}, but failed to index them in database.\nThe failed ones will be overwritten on next import")
        }
        return importedFiles.map { it.first }
    }
}