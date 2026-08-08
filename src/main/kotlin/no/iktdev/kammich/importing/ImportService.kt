package no.iktdev.kammich.importing

import kotlinx.coroutines.*
import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.getDeviceId
import no.iktdev.kammich.errorNotification
import no.iktdev.kammich.infoNotification
import no.iktdev.kammich.models.internal.DeviceReadyEvent
import no.iktdev.kammich.models.internal.KFile
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
import no.iktdev.kammich.toXxHash
import no.iktdev.kammich.warningNotification
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.Instant
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
    private val importScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val importStartedMap = ConcurrentHashMap<String, Instant>()
    private val importDeviceNameMap = ConcurrentHashMap<String, String>()
    private val importList = ConcurrentHashMap<String, List<InternalImportFile>>()

    @EventListener
    fun onDeviceReady(event: DeviceReadyEvent) {
        val device = event.device
        if (!isAllowedToImport(device.id)) {
            log.debug("Device {} not allowed to import {}", device.id, device.sysPath)
            return
        }
        indexDevice(device)
    }

    fun getAllActiveImports(): List<DeviceImport> {
        return importList.keys.mapNotNull { deviceIdStr ->
            val internalFiles = importList[deviceIdStr] ?: return@mapNotNull null
            val started = importStartedMap[deviceIdStr] ?: Instant.now()
            val deviceName = importDeviceNameMap[deviceIdStr] ?: deviceIdStr

            val sharedFiles = internalFiles.map {
                SharedImportFile(
                    file = it.file.path,
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

    fun indexDevice(device: RemovableDevice) {
        log.info("Starting indexing for device ${device.name}")

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
                file = it.file.path,
                isNew = true,
                state = it.state
            )
        }

        val completedCount = sharedFiles.count { it.state == FileImportState.Success || it.state == FileImportState.Failure }
        val currentIndex = sharedFiles.indexOfFirst { it.state == FileImportState.InProgress }
        val currentFile = if (currentIndex != -1) sharedFiles[currentIndex].file else null

        val windowFiles = if (currentIndex != -1) {
            val fromIndex = maxOf(0, currentIndex - 2)
            val toIndex = minOf(sharedFiles.size, currentIndex + 3)
            sharedFiles.subList(fromIndex, toIndex)
        } else {
            sharedFiles.take(5)
        }

        val progressEvent = ImportProgressEvent(
            deviceId = deviceIdStr,
            completedFiles = completedCount,
            totalFiles = sharedFiles.size,
            currentFile = currentFile,
            state = if (completedCount == sharedFiles.size) FileImportState.Success else FileImportState.InProgress,
            files = windowFiles
        )

        sseManager.send(SSEImportMediaProgress(progressEvent))

        // Sender også oppdatert global enhetsstatus via SSE
        broadcastDeviceState()
    }

    private fun broadcastDeviceState() {
        val summaries = importList.keys.map { deviceIdStr ->
            val internalFiles = importList[deviceIdStr] ?: emptyList()
            val startedInstant = importStartedMap[deviceIdStr] ?: Instant.now()

            val hasStarted = internalFiles.any { it.state != FileImportState.Pending }
            val isCompleted = internalFiles.isNotEmpty() && internalFiles.all { it.state == FileImportState.Success || it.state == FileImportState.Failure }

            val state = when {
                isCompleted -> ImportState.Completed
                hasStarted -> ImportState.Importing
                else -> ImportState.Started
            }

            DeviceImportSummary(
                deviceId = deviceIdStr,
                state = state,
                started = startedInstant.toString(),
                completed = if (isCompleted) Instant.now().toString() else ""
            )
        }

        sseManager.send(SSEImportState(summaries))
    }

    fun startImportForDevice(device: RemovableDevice, files: List<KFile>) {
        val storage = fileRepository.getStorageLocationForImport(device) ?: return
        val dbId = getDeviceIdToImport(device) ?: return
        val provider = providerFactory.getProvider(device)
        val deviceIdStr = device.id

        cancelImport(deviceIdStr)

        importStartedMap[deviceIdStr] = Instant.now()
        importDeviceNameMap[deviceIdStr] = device.name ?: device.id
        importList[deviceIdStr] = files.map { InternalImportFile(it, FileImportState.Pending) }

        // Send første statusoppdatering for enheten
        broadcastDeviceState()

        val job = importScope.launch {
            try {
                files.forEach { file ->
                    ensureActive()

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

                    if (imported != null) {
                        try {
                            val hash = imported.toXxHash()
                            fileRepository.saveFile(dbId, imported, ZonedDateTime.now(), hash)
                            updateFileState(deviceIdStr, file.id, FileImportState.Success)
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
                finishImport(deviceIdStr)
            }
        }

        activeImportJobs[deviceIdStr] = job
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
        importList.remove(deviceIdStr)
        importStartedMap.remove(deviceIdStr)
        importDeviceNameMap.remove(deviceIdStr)
        log.warn("Kansellerte import for enhet {}", deviceIdStr)

        // Oppdater klientene om at enheten er fjernet/avbrutt
        broadcastDeviceState()
    }

    private fun finishImport(deviceIdStr: String) {
        val finalFiles = importList[deviceIdStr] ?: emptyList()
        val successCount = finalFiles.count { it.state == FileImportState.Success }
        val failedFiles = finalFiles.filter { it.state == FileImportState.Failure }
        val failCount = failedFiles.size

        importList.remove(deviceIdStr)
        importStartedMap.remove(deviceIdStr)
        importDeviceNameMap.remove(deviceIdStr)

        // Oppdater klientene etter at ryddejobben er ferdig
        broadcastDeviceState()

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