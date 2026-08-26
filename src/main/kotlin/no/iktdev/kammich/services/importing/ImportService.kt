package no.iktdev.kammich.services.importing

import kotlinx.coroutines.*
import no.iktdev.kammich.services.ConfigService
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.getDeviceId
import no.iktdev.kammich.errorNotification
import no.iktdev.kammich.infoNotification
import no.iktdev.kammich.models.internal.events.DeviceReadyEvent
import no.iktdev.kammich.models.internal.KFile
import no.iktdev.kammich.models.internal.events.ImportJobCompletedEvent
import no.iktdev.kammich.models.shared.DeviceImport
import no.iktdev.kammich.models.shared.DeviceImportSummary
import no.iktdev.kammich.models.internal.ImportFile as InternalImportFile
import no.iktdev.kammich.models.shared.ImportFile as SharedImportFile
import no.iktdev.kammich.models.shared.ImportProgressEvent
import no.iktdev.kammich.models.shared.FileImportState
import no.iktdev.kammich.models.shared.ImportState
import no.iktdev.kammich.models.shared.device.RemovableDevice
import no.iktdev.kammich.repository.FileRepository
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.SSEImportMediaProgress
import no.iktdev.kammich.sse.events.SSEImportState
import no.iktdev.kammich.storage.DeviceManagerService
import no.iktdev.kammich.storage.provider.DeviceUnavailableException
import no.iktdev.kammich.storage.provider.StorageProviderFactory
import no.iktdev.kammich.toSha1
import no.iktdev.kammich.warningNotification
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.time.Instant
import java.time.ZonedDateTime
import java.util.UUID
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
    private val importScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val importStartedMap = ConcurrentHashMap<String, Instant>()
    private val importDeviceNameMap = ConcurrentHashMap<String, String>()
    private val importList = ConcurrentHashMap<String, List<InternalImportFile>>()

    @Order(999)
    @EventListener
    fun onDeviceReady(event: DeviceReadyEvent) {
        val device = event.device

        if (device.isReady) {
            log.info("Device ready: $device")
        } else {
            log.warn("Device ${device.name} not ready")
            return
        }

        if (!isAllowedToImport(device.id)) {
            log.debug("Device {} not allowed to import {}", device.id, device.sysPath)
            return
        }

        // Start coroutinen og lagre jobben umiddelbart i activeImportJobs
        // slik at kansellering fungerer under hele prosessen (inkludert indeks-fasen).
        val job = importScope.launch {
            indexDevice(device)
        }

        activeImportJobs[device.id] = job
    }

    fun getAllActiveImports(): List<DeviceImport> {
        return importList.keys.mapNotNull { deviceIdStr ->
            val internalFiles = importList[deviceIdStr] ?: return@mapNotNull null
            val started = importStartedMap[deviceIdStr] ?: Instant.now()
            val deviceName = importDeviceNameMap[deviceIdStr] ?: deviceIdStr

            val sharedFiles = internalFiles.map {
                SharedImportFile(
                    file = Path.of(it.file.path, it.file.name).toString(),
                    isNew = true,
                    state = it.state
                )
            }

            val completedCount = sharedFiles.count { it.state == FileImportState.Success || it.state == FileImportState.Failure }
            val currentFile = internalFiles.firstOrNull { it.state == FileImportState.InProgress }?.file?.path

            DeviceImport(
                deviceId = deviceIdStr,
                deviceName = deviceName,
                started = started,
                totalFiles = sharedFiles.size,
                completedFiles = completedCount,
                currentFileName = currentFile,
                files = sharedFiles
            )
        }
    }

    fun isAllowedToImport(deviceId: String) =
        deviceManager.getSettings(deviceId).autoImport == true

    suspend fun indexDevice(device: RemovableDevice) {
        val deviceSN = device.id
        log.info("Starting indexing for device ${device.name}")

        importStartedMap[deviceSN] = Instant.now()
        importDeviceNameMap[deviceSN] = device.name
        importList[deviceSN] = emptyList()

        // Eksplisitt broadcast for indexing-fase
        broadcastDeviceState(ImportState.Indexing)

        val filesToImport = try {
            deviceContentIndexing.getNewFilesToImport(device)
        } catch (e: Exception) {
            log.error("Feil under indeksering av enhet {}", deviceSN, e)
            finishImport(deviceSN)
            return
        }

        if (filesToImport.isEmpty()) {
            log.info("No new files to import for ${device.name}")
            eventPublisher.infoNotification("ImportService-NoFiles-${device.id}", "No files to import", "All files have already been imported for ${device.model}")
            finishImport(device.id)
            return
        }

        log.info("Found ${filesToImport.size} new files to import")
        val config = configService.getConfig().deviceSettings[device.id]
        if (config?.autoImport != true) {
            log.error("Device ${device.id} has auto-import disabled")
            finishImport(device.id)
            return
        }
        startImportForDevice(device, filesToImport)
    }

    private fun getDeviceIdToImport(device: RemovableDevice): Long? {
        return DevicesTable.getDeviceId(device.id) ?: run {
            eventPublisher.warningNotification("ImportService-Device-not-present-${device.id}",
                "Device ${device.id} missing",
                "Device ${device.id} is not stored in database!\nCan't import files untill this is resolved",
            )
            return null
        }
    }

    private fun updateFileState(deviceIdStr: String, fileId: String, newState: FileImportState) {
        importList.computeIfPresent(deviceIdStr) { _, currentList ->
            currentList.map { importFile ->
                if (importFile.file.id == fileId) {
                    importFile.copy(state = newState)
                } else {
                    importFile
                }
            }
        }
        broadcastProgress(deviceIdStr)
    }

    private fun broadcastProgress(deviceIdStr: String) {
        val internalFiles = importList[deviceIdStr] ?: return
        val sharedFiles = internalFiles.map {
            SharedImportFile(
                file = Path.of(it.file.path, it.file.name).toString(),
                isNew = true,
                state = it.state
            )
        }

        val completedCount = sharedFiles.count { it.state == FileImportState.Success || it.state == FileImportState.Failure }

        // Finn aktiv indeks, eller bruk den sist fullførte. Hvis ingenting har skjedd enna (alt er Pending), blir targetIndex 0.
        val activeIndex = sharedFiles.indexOfFirst { it.state == FileImportState.InProgress }
        val targetIndex = if (activeIndex != -1) {
            activeIndex
        } else {
            maxOf(0, completedCount - 1)
        }

        val windowFiles = if (sharedFiles.isNotEmpty()) {
            // Sikrer at vi i starten (eller rundt target) forsøker å vise et fint vindu på opptil 5 filer
            val fromIndex = maxOf(0, targetIndex - 2)
            val toIndex = minOf(sharedFiles.size, maxOf(5, targetIndex + 3))
            sharedFiles.subList(fromIndex, toIndex)
        } else {
            emptyList()
        }

        val progressEvent = ImportProgressEvent(
            deviceId = deviceIdStr,
            completedFiles = completedCount,
            totalFiles = sharedFiles.size,
            currentFile = if (activeIndex != -1) sharedFiles[activeIndex].file else null,
            state = if (completedCount == sharedFiles.size) FileImportState.Success else FileImportState.InProgress,
            files = windowFiles
        )

        sseManager.send(SSEImportMediaProgress(progressEvent))
    }


    private fun broadcastDeviceState(impliedState: ImportState = ImportState.Importing) {
        val summaries = importStartedMap.keys.map { deviceIdStr ->
            val internalFiles = importList[deviceIdStr] ?: emptyList()
            val startedInstant = importStartedMap[deviceIdStr] ?: Instant.now()

            val isCompleted = internalFiles.isNotEmpty() && internalFiles.all { it.state == FileImportState.Success || it.state == FileImportState.Failure }

            val state = when {
                isCompleted -> ImportState.Completed
                // Hvis den blir trigget med Canceled-state, settes den her:
                !activeImportJobs.containsKey(deviceIdStr) && !isCompleted -> ImportState.Canceled
                else -> impliedState
            }

            DeviceImportSummary(
                deviceId = deviceIdStr,
                state = state,
                started = startedInstant.toString(),
                completed = if (isCompleted || state == ImportState.Canceled) Instant.now().toString() else ""
            )
        }

        sseManager.send(SSEImportState(summaries))
    }

    suspend fun startImportForDevice(device: RemovableDevice, files: List<KFile>) {
        val importJobId = UUID.randomUUID()
        val storage = fileRepository.getStorageLocationForImport(device) ?: return
        val dbId = getDeviceIdToImport(device) ?: return
        val provider = providerFactory.getProvider(device)
        val deviceIdStr = device.id

        importList[deviceIdStr] = files.map { InternalImportFile(it, FileImportState.Pending) }

        // Går over fra Indexing til Importing her ved å kalle uten parameter (isIndexing blir false)
        broadcastDeviceState()



        try {
            files.forEach { file ->
                currentCoroutineContext().ensureActive()

                updateFileState(deviceIdStr, file.id, FileImportState.InProgress)

                val imported = try {
                    provider.copyFile(device, storage, file)
                } catch (e: DeviceUnavailableException) {
                    updateFileState(deviceIdStr, file.id, FileImportState.Failure)
                    log.error("Kamera utilgjengelig under kopiering av fil {}. Avbryter hele importen.", file, e)
                    throw e
                } catch (e: Exception) {
                    log.error("Feil ved kopiering av fil {}, fortsetter med neste.", file, e)
                    null
                }

                if (imported != null && imported.exists()) {
                    if (imported.length() != file.size) {
                        log.error(
                            "Imported file size mismatch for {}. Expected {}, got {}",
                            file,
                            file.size,
                            imported.length()
                        )
                        updateFileState(deviceIdStr, file.id, FileImportState.Failure)
                        return@forEach
                    }

                    try {
                        val hash = imported.toSha1()
                        val persistedFile = fileRepository.saveFile(dbId, imported, ZonedDateTime.now(), hash, importJobId, file)
                        if (persistedFile != null) {
                            updateFileState(deviceIdStr, file.id, FileImportState.Success)
                        } else {
                            updateFileState(deviceIdStr, file.id, FileImportState.Failure)
                        }
                    } catch (e: Exception) {
                        log.error("Failed to save file {} to database", imported, e)
                        updateFileState(deviceIdStr, file.id, FileImportState.Failure)
                    }
                } else {
                    updateFileState(deviceIdStr, file.id, FileImportState.Failure)
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
            finishImport(deviceIdStr, importJobId)
        }
    }

    fun cancelAllImports() {
        val ids = activeImportJobs.keys.toList()
        ids.forEach { jobId ->
            cancelImport(jobId)
        }
    }

    fun cancelImport(deviceIdStr: String) {
        activeImportJobs[deviceIdStr]?.cancel()
        activeImportJobs.remove(deviceIdStr)

        // 1. Send ut SSE-status om at denne enheten nå er Canceled
        broadcastDeviceState(ImportState.Canceled)

        // 2. Rydd opp i minnet etter at staten er broadcastet (eventuelt behold litt på samme måte som finishImport)
        importList.remove(deviceIdStr)
        importStartedMap.remove(deviceIdStr)
        importDeviceNameMap.remove(deviceIdStr)

        log.warn("Kansellerte import for enhet {}", deviceIdStr)
    }

    private fun finishImport(deviceSN: String, importJobId: UUID? = null) {
        val finalFiles = importList[deviceSN] ?: emptyList()
        val successCount = finalFiles.count { it.state == FileImportState.Success }
        val failedFiles = finalFiles.filter { it.state == FileImportState.Failure }
        val failCount = failedFiles.size

        broadcastDeviceState(ImportState.Completed)

        importList.remove(deviceSN)
        importStartedMap.remove(deviceSN)
        importDeviceNameMap.remove(deviceSN)

        if (successCount > 0) {
            eventPublisher.infoNotification(
                "ImportService-Success-$deviceSN",
                "Import ferdig",
                "Importerte $successCount filer fra enhet $deviceSN. ${if (failCount > 0) "($failCount feilet)" else ""}"
            )
            log.info("Import fullført for $deviceSN: $successCount suksesser, $failCount feil.")
        } else if (finalFiles.isNotEmpty()) {
            eventPublisher.warningNotification(
                "ImportService-Failed-$deviceSN",
                "Import feilet",
                "Ingen filer ble importert. $failCount feilet."
            )
        }
        importJobId?.let { jobId ->
            eventPublisher.publishEvent(ImportJobCompletedEvent(jobId, deviceSN))
        }
    }
}