package no.iktdev.kammich.importing

import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.models.internal.KFile
import no.iktdev.kammich.models.shared.device.RemovableDevice
import no.iktdev.kammich.repository.FileRepository
import no.iktdev.kammich.storage.DeviceManagerService
import no.iktdev.kammich.storage.provider.StorageProvider
import no.iktdev.kammich.storage.provider.StorageProviderFactory
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Component
import kotlin.plus

@Component
class DeviceContentIndexing(
    private val deviceManager: DeviceManagerService,
    private val fileRepository: FileRepository,
    private val providerFactory: StorageProviderFactory,
) {
    private val log = LoggerFactory.getLogger(DeviceContentIndexing::class.java)

    fun getDeclaredForImport(deviceId: String) =
        deviceManager.getSettings(deviceId).includeFolders ?: emptyList()


    fun getDeclaredToExclude(deviceId: String) =
        deviceManager.getSettings(deviceId).excludeFolders ?: emptyList()


    fun getDCIM(device: RemovableDevice, provider: StorageProvider): List<KFile> {
        log.debug("Fishing for DCIM folder at ${device.model ?: device.name}")
        return provider.let { provider ->
            provider.getDCIM(device)?.let { dcim ->
                log.debug("Caught a DCIM folder at ${dcim.path}")
                provider.listAllFiles(device, dcim.path)
            }
        } ?: emptyList()
    }

    fun getFilesToImport(device: RemovableDevice): List<KFile> {
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

    fun getNewFilesToImport(device: RemovableDevice): List<KFile> {
        val foundFiles = getFilesToImport(device)
        return fileRepository.getFilesToImport(device.id, foundFiles)
    }
}