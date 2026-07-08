package no.iktdev.kammich.storage

import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.getDeviceId
import no.iktdev.kammich.ensureWritable
import no.iktdev.kammich.infoNotification
import no.iktdev.kammich.models.files.KFile
import no.iktdev.kammich.models.shared.storage.internal.DeviceDetectedEvent
import no.iktdev.kammich.models.shared.storage.removable.Device
import no.iktdev.kammich.repository.FileRepository
import no.iktdev.kammich.storage.provider.StorageProvider
import no.iktdev.kammich.storage.provider.StorageProviderFactory
import no.iktdev.kammich.warningNotification
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.io.File
import java.time.ZonedDateTime

@Service
class ImportService(
    private val deviceManager: DeviceManagerService,
    private val configService: ConfigService,
    private val fileRepository: FileRepository,
    private val providerFactory: StorageProviderFactory,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(ImportService::class.java)

    @EventListener
    fun onDeviceDetected(event: DeviceDetectedEvent) {
        val device = deviceManager.getDeviceBySysPath(event.sysPath) // Eller stableId
        if (device != null) {
            if (!isAllowedToImport(device.id)) {
                log.debug("Device ${device.id} not allowed to import ${event.sysPath}")
                return
            }
            indexDevice(device)
        } else {
            log.info("Device ${event.sysPath} not found")
        }
    }

    fun getDeclaredForImport(deviceId: String): List<String> {
        return deviceManager.getSettings(deviceId).includeFolders ?: emptyList()
    }

    fun getDeclaredToExclude(deviceId: String): List<String> {
        return deviceManager.getSettings(deviceId).excludeFolders ?: emptyList()
    }

    fun isAllowedToImport(deviceId: String): Boolean {
        return deviceManager.getSettings(deviceId).autoImport == true
    }

    fun getDCIM(device: Device, provider: StorageProvider): List<KFile> {
        log.info("Looking for DCIM folder for ${device.model ?: device.name}")
        return provider.let { provider ->
            provider.getDCIM(device)?.let { dcim -> provider.listAllFiles(device, dcim.path) }
        } ?: emptyList()
    }

    fun getFilesToImport(device: Device): List<KFile> {
        val provider = providerFactory.getProvider(device)

        // 1. Hent alle potensielle filer
        val dcimContent = getDCIM(device, provider)
        val addContent = getDeclaredForImport(device.id).flatMap { provider.listAllFiles(device, it) }
        val allPotentialFiles = dcimContent + addContent

        val toExclude = getDeclaredToExclude(device.id)

        // 2. Logg og filtrer ekskluderinger
        val eligibleFiles = allPotentialFiles.mapNotNull { file ->
            val excludedPath = toExclude.find { file.path.startsWith(it) }
            if (excludedPath != null) {
                log.info("FILE [EXCLUDED] ${file.name} (matched path: $excludedPath)")
                null // Fjernes
            } else {
                file // Beholdes for videre sjekk
            }
        }

        // 3. Sjekk mot database (hva er allerede importert?)
        val toImport = fileRepository.getFilesToImport(device.id, eligibleFiles)

        // 4. Logg resultatet
        eligibleFiles.forEach { file ->
            val status = if (file in toImport) "QUEUED FOR IMPORT" else "ALREADY PRESENT"
            log.info("FILE [$status] ${file.name} (path: ${file.path})")
        }

        log.info("Indexing summary: Found ${allPotentialFiles.size} total, ${toExclude.size} paths excluded, ${toImport.size} ready for import.")

        return toImport
    }


    fun indexDevice(device: Device) {
        log.info("Starting indexing for device ${device.name}")

        // Bruk den nye logikken som kombinerer DCIM + Include + Exclude
        val filesToImport = getFilesToImport(device)

        // Nå sjekker vi kun mot DB for å se hva som faktisk er NYTT
        val newFiles = fileRepository.getFilesToImport(device.id, filesToImport)

        if (newFiles.isEmpty()) {
            log.info("No new files to import for ${device.name}")
            return
        }

        log.info("Found ${newFiles.size} new files to import")
        importFiles(device, newFiles)
    }

    fun importFiles(device: Device, files: List<KFile>): List<File> {
        val config = configService.getConfig().deviceSettings[device.id]
        if (config?.autoImport != true) {
            log.error("Device ${device.id} has auto-import disabled")
            return emptyList()
        }

        val provider = providerFactory.getProvider(device)
        log.info("Config: ${configService.getConfig()}")
        val mediaRoot = File(configService.getConfig().mediaPath).ensureWritable(
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