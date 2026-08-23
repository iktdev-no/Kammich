package no.iktdev.kammich.services.importing

import kotlinx.coroutines.*
import no.iktdev.kammich.services.ConfigService
import no.iktdev.kammich.infoNotification
import no.iktdev.kammich.models.internal.KFile
import no.iktdev.kammich.models.internal.KFileType
import no.iktdev.kammich.models.shared.DeviceImportSummary
import no.iktdev.kammich.models.internal.ImportFile as InternalImportFile
import no.iktdev.kammich.models.shared.ImportFile as SharedImportFile
import no.iktdev.kammich.models.shared.ImportProgressEvent
import no.iktdev.kammich.models.shared.FileImportState
import no.iktdev.kammich.models.shared.ImportState
import no.iktdev.kammich.models.shared.device.DeviceInterfaceType
import no.iktdev.kammich.models.shared.device.DeviceType
import no.iktdev.kammich.models.shared.device.GPhoto2Device
import no.iktdev.kammich.models.shared.device.RemovableDevice
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.SSEImportMediaProgress
import no.iktdev.kammich.sse.events.SSEImportState
import no.iktdev.kammich.warningNotification
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random


@Service
class MockImportService(
    private val sseManager: SseManager,
    private val configService: ConfigService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(MockImportService::class.java)
    private val activeImportJobs = ConcurrentHashMap<String, Job>()
    private val importScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val importStartedMap = ConcurrentHashMap<String, Instant>()
    private val importDeviceNameMap = ConcurrentHashMap<String, String>()
    private val importList = ConcurrentHashMap<String, List<InternalImportFile>>()



    fun mockImportOf(filesCount: Int?) {
        val totalFiles = filesCount ?: Random.nextInt(69, 420)

        // Oppdater dummy-genereringen i mockDeviceIndexing til å bruke totalFiles,
        // eller send den med som parameter:
        mockDeviceIndexing(totalFiles)
    }

    fun mockDeviceIndexing(fileCount: Int = 5) {
        val mockDevice = GPhoto2Device(
            id = "mock-device-123",
            name = "Mock Kamera",
            sysPath = "/mock/path",
            model = "TestCam",
            manufacturer = "mock",
            sn = "mock",
            port = "",
            interfaceType = DeviceInterfaceType.UNKNOWN,
            storage = emptyList(),
            deviceType = DeviceType.Unknown,
            isReady = true
        )

        importScope.launch {
            val deviceIdStr = mockDevice.id
            log.info("Starter mock indexing for enhet {} med {} filer", mockDevice.name, fileCount)

            importStartedMap[deviceIdStr] = Instant.now()
            importDeviceNameMap[deviceIdStr] = mockDevice.name ?: deviceIdStr
            importList[deviceIdStr] = emptyList()

            broadcastDeviceState(ImportState.Indexing)

            delay(2000)

            val dummyFiles = (1..fileCount).map { i ->
                KFile(
                    id = "file-$i",
                    path = "DCIM/100MEDIA",
                    name = "PIC_${i.toString().padStart(4, '0')}.JPG",
                    type = KFileType.FILE,
                    size = 0
                )
            }

            log.info("Fant {} mock-filer, starter import", dummyFiles.size)
            startImportForDevice(mockDevice, dummyFiles)
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
                else -> impliedState
            }

            DeviceImportSummary(
                deviceId = deviceIdStr,
                state = state,
                started = startedInstant.toString(),
                completed = if (isCompleted) Instant.now().toString() else ""
            )
        }
        log.info("Broadcast device state {}", summaries)
        sseManager.send(SSEImportState(summaries))
    }

    fun startImportForDevice(device: RemovableDevice, files: List<KFile>) {
        val deviceIdStr = device.id

        importList[deviceIdStr] = files.map { InternalImportFile(it, FileImportState.Pending) }

        // Går over fra Indexing til Importing
        broadcastDeviceState()

        val job = importScope.launch {
            try {
                files.forEach { file ->
                    ensureActive()

                    // Sett til InProgress
                    updateFileState(deviceIdStr, file.id, FileImportState.InProgress)
                    delay(1000) // Simuler kopieringstid

                    // Sett til Success (eller simuler feil på noen om ønskelig)
                    updateFileState(deviceIdStr, file.id, FileImportState.Success)
                    delay(500)
                }
            } catch (e: CancellationException) {
                log.info("Mock import job ble avbrutt for enhet {}", deviceIdStr)
                throw e
            } catch (e: Exception) {
                log.error("Uventet feil under mock import-loop for enhet {}", deviceIdStr, e)
            } finally {
                activeImportJobs.remove(deviceIdStr)
                finishImport(deviceIdStr)
            }
        }

        activeImportJobs[deviceIdStr] = job
    }

    private fun finishImport(deviceIdStr: String) {
        val finalFiles = importList[deviceIdStr] ?: emptyList()
        val successCount = finalFiles.count { it.state == FileImportState.Success }
        val failedFiles = finalFiles.filter { it.state == FileImportState.Failure }
        val failCount = failedFiles.size

        broadcastDeviceState(ImportState.Completed)


        importList.remove(deviceIdStr)
        importStartedMap.remove(deviceIdStr)
        importDeviceNameMap.remove(deviceIdStr)


        if (successCount > 0) {
            eventPublisher.infoNotification(
                "ImportService-Success-$deviceIdStr",
                "Import ferdig",
                "Importerte $successCount filer fra enhet $deviceIdStr. ${if (failCount > 0) "($failCount feilet)" else ""}"
            )
            log.info("Import fullført for $deviceIdStr: $successCount suksesser, $failCount feil.")
        } else if (finalFiles.isNotEmpty()) {
            eventPublisher.warningNotification(
                "ImportService-Failed-$deviceIdStr",
                "Import feilet",
                "Ingen filer ble importert. $failCount feilet."
            )
        }
    }

}