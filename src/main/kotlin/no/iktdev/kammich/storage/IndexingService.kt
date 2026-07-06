package no.iktdev.kammich.storage

import no.iktdev.kammich.models.shared.storage.internal.DeviceDetectedEvent
import no.iktdev.kammich.models.shared.storage.removable.Device
import no.iktdev.kammich.repository.FileRepository
import no.iktdev.kammich.storage.provider.StorageProviderFactory
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class IndexingService(
    private val deviceManager: DeviceManagerService,
    private val providerFactory: StorageProviderFactory,
    private val fileRepository: FileRepository,
    private val importService: ImportService,
) {
    private val log = LoggerFactory.getLogger(IndexingService::class.java)

    @EventListener
    fun onDeviceDetected(event: DeviceDetectedEvent) {
        log.info("Received device detected event: $event, will look for DCIM")
        val device = deviceManager.getDeviceBySysPath(event.sysPath) // Eller stableId
        if (device != null) {
            indexDevice(device)
        } else {
            log.info("Device ${event.sysPath} not found")
        }
    }

    fun indexDevice(device: Device) {
        log.info("Starting indexing for device ${device.name}")
        val provider = providerFactory.getProvider(device)
        val dcim = provider.getDCIM(device)
        if (dcim == null) {
            log.info("No dcim found for ${device.name}")
            return
        }
        log.info("Fant DCIM i ${dcim.path}")
        val files = provider.listAllFiles(device, dcim.path)
        val toImport = fileRepository.getFilesToImport(device.id, files)
        files.forEach { file ->
            val status = if (file in toImport) "Import" else "Present"
            log.info("Found file [$status] ${file.name}")
        }
        importService.importFiles(device, toImport)
    }
}