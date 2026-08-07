package no.iktdev.kammich.importing

import kotlinx.coroutines.*
import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.getDeviceId
import no.iktdev.kammich.errorNotification
import no.iktdev.kammich.infoNotification
import no.iktdev.kammich.models.internal.DeviceReadyEvent
import no.iktdev.kammich.models.internal.KFile
import no.iktdev.kammich.models.internal.ImportFile
import no.iktdev.kammich.models.internal.ImportState
import no.iktdev.kammich.models.shared.device.RemovableDevice
import no.iktdev.kammich.repository.FileRepository
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.storage.DeviceManagerService
import no.iktdev.kammich.storage.provider.DeviceUnavailableException
import no.iktdev.kammich.storage.provider.StorageProviderFactory
import no.iktdev.kammich.toXxHash
import no.iktdev.kammich.warningNotification
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

@Service
class ImportService(
    private val sseManager: SseManager,
    private val deviceManager: DeviceManagerService,
    private val configService: ConfigService,
    private val fileRepository: FileRepository,
    private val providerFactory: StorageProviderFactory,
    private val eventPublisher: ApplicationEventPublisher,
    private val deviceContentIndexing: DeviceContentIndexing
) {
    private val log = LoggerFactory.getLogger(ImportService::class.java)
    private val activeImportJobs = ConcurrentHashMap<String, Job>()

    // Egen scope for import-tjenesten (eller du kan injisere en felles Dispatcher/Scope)
    private val importScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @EventListener
    fun onDeviceReady(event: DeviceReadyEvent) {
        val device = event.device
        if (!isAllowedToImport(device.id)) {
            log.debug("Device {} not allowed to import {}", device.id, device.sysPath)
            return
        }
        indexDevice(device)
    }

    fun isAllowedToImport(deviceId: String) =
        deviceManager.getSettings(deviceId).autoImport == true


    fun indexDevice(device: RemovableDevice) {
        log.info("Starting indexing for device ${device.name}")

        // Bruk den nye logikken som kombinerer DCIM + Include + Exclude
        val filesToImport = deviceContentIndexing.getNewFilesToImport(device)

        if (filesToImport.isEmpty()) {
            log.info("No new files to import for ${device.name}")
            eventPublisher.infoNotification("ImportService-NoFiles-${device.id}", "No files to import", "All files have already been imported for ${device.model ?: device.id}")
            return
        }

        log.info("Found ${filesToImport.size} new files to import")
        val config = configService.getConfig().deviceSettings[device.id]
        if (config?.autoImport != true) {
            log.error("Device ${device.id} has auto-import disabled")
            return
        }
        startImportForDevice(device, filesToImport)
    }

    private fun getDeviceIdToImport(device: RemovableDevice): Int? {
        return DevicesTable.getDeviceId(device.id) ?: run {
            eventPublisher.warningNotification("ImportService-Device-not-present-${device.id}",
                "Device ${device.id} missing",
                "Device ${device.id} is not stored in database!\nCan't import files untill this is resolved",
            )
            return null
        }
    }


    private val importList = ConcurrentHashMap<String, List<ImportFile>>()
    private fun updateFileState(deviceIdStr: String, fileId: String, newState: ImportState) {
        // computeIfPresent låser oppdateringen trygt internt i map-en
        importList.computeIfPresent(deviceIdStr) { _, currentList ->
            currentList.map { importFile ->
                if (importFile.file.id == fileId) {
                    // Returner en ny instans med oppdatert state
                    importFile.copy(state = newState)
                } else {
                    importFile
                }
            }
        }
    }

    fun startImportForDevice(device: RemovableDevice, files: List<KFile>) {
        val storage = fileRepository.getStorageLocationForImport(device) ?: return
        val dbId = getDeviceIdToImport(device) ?: return
        val provider = providerFactory.getProvider(device)
        val deviceIdStr = device.id

        // Avbryt eventuell eksisterende import for denne enheten først
        cancelImport(deviceIdStr)

        // Initialiser listen med Pending
        importList[deviceIdStr] = files.map { ImportFile(it, ImportState.Pending) }

        val job = importScope.launch {
            try {
                files.forEach { file ->
                    ensureActive() // Støtte for korutine-kansellering

                    updateFileState(deviceIdStr, file.id, ImportState.InProgress)

                    val imported = try {
                        provider.copyFile(device, storage, file)
                    } catch (e: DeviceUnavailableException) {
                        updateFileState(deviceIdStr, file.id, ImportState.Failure)
                        log.error("Kamera utilgjengelig under kopiering av fil {}. Avbryter hele importen.", file, e)
                        throw e // Sender unntaket opp til hoved-try/catch som stopper loopen
                    } catch (e: Exception) {
                        log.error("Feil ved kopiering av fil {}, fortsetter med neste.", file, e)
                        null
                    }

                    if (imported != null) {
                        try {
                            val hash = imported.toXxHash()
                            fileRepository.saveFile(dbId, imported, ZonedDateTime.now(), hash)
                            updateFileState(deviceIdStr, file.id, ImportState.Success)
                        } catch (e: Exception) {
                            log.error("Failed to save file {} to database", imported, e)
                            updateFileState(deviceIdStr, file.id, ImportState.Failure)
                        }
                    } else {
                        updateFileState(deviceIdStr, file.id, ImportState.Failure)
                    }
                }
            } catch (e: DeviceUnavailableException) {
                eventPublisher.errorNotification("ImportService-Disconnected-${device.id}", "${device.model} frakoblet", e.message ?: e.localizedMessage)
                throw e
            } catch (e: CancellationException) {
                log.info("Import job ble avbrutt for enhet {}", deviceIdStr)
                throw e
            } catch (e: Exception) {
                log.error("Uventet feil under import-loop for enhet {}", deviceIdStr, e)
            } finally {
                activeImportJobs.remove(deviceIdStr)
                finishImport(deviceIdStr)
            }
        }

        activeImportJobs[deviceIdStr] = job
    }

    fun cancelImport(deviceIdStr: String) {
        activeImportJobs[deviceIdStr]?.cancel()
        activeImportJobs.remove(deviceIdStr)
        importList.remove(deviceIdStr)
        log.warn("Kansellerte import for enhet {}", deviceIdStr)
    }



    private fun finishImport(deviceIdStr: String) {
        val finalFiles = importList[deviceIdStr] ?: emptyList()
        val successCount = finalFiles.count { it.state == ImportState.Success }
        val failedFiles = finalFiles.filter { it.state == ImportState.Failure }
        val failCount = failedFiles.size

        importList.remove(deviceIdStr)

        // 3. Send notifikasjon basert på resultatet
        if (successCount > 0) {
            eventPublisher.infoNotification(
                "ImportService-Success-$deviceIdStr",
                "Import ferdig",
                "Importerte $successCount filer fra enhet $deviceIdStr. ${if (failCount > 0) "($failCount feilet)" else ""}"
            )
            log.info("Import fullført for $deviceIdStr: $successCount suksesser, $failCount feil.")
        } else {
            eventPublisher.warningNotification(
                "ImportService-Failed-$deviceIdStr",
                "Import feilet",
                "Ingen filer ble importert. $failCount feilet."
            )
        }
    }

}