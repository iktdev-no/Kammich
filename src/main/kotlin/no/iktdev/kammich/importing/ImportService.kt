package no.iktdev.kammich.importing

import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.getDeviceId
import no.iktdev.kammich.ensureWritable
import no.iktdev.kammich.gphoto2.GPhoto2
import no.iktdev.kammich.infoNotification
import no.iktdev.kammich.models.internal.DeviceReadyEvent
import no.iktdev.kammich.models.internal.KFile
import no.iktdev.kammich.models.shared.device.RemovableDevice
import no.iktdev.kammich.repository.FileRepository
import no.iktdev.kammich.storage.DeviceManagerService
import no.iktdev.kammich.storage.provider.StorageProvider
import no.iktdev.kammich.storage.provider.StorageProviderFactory
import no.iktdev.kammich.warningNotification
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import java.io.File
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicInteger

@Service
class ImportService(
    private val deviceManager: DeviceManagerService,
    private val configService: ConfigService,
    private val fileRepository: FileRepository,
    private val providerFactory: StorageProviderFactory,
    private val eventPublisher: ApplicationEventPublisher,
    private val taskScheduler: TaskScheduler,
    private val deviceContentIndexing: DeviceContentIndexing
) {
    private val log = LoggerFactory.getLogger(ImportService::class.java)

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
        val filesToImport = deviceContentIndexing.getFilesToImport(device)

        // Nå sjekker vi kun mot DB for å se hva som faktisk er NYTT
        val newFiles = fileRepository.getFilesToImport(device.id, filesToImport)

        if (newFiles.isEmpty()) {
            log.info("No new files to import for ${device.name}")
            eventPublisher.infoNotification("ImportService-NoFiles-${device.id}", "No files to import", "All files have already been imported for ${device.model ?: device.id}")
            return
        }

        log.info("Found ${newFiles.size} new files to import")
        val config = configService.getConfig().deviceSettings[device.id]
        if (config?.autoImport != true) {
            log.error("Device ${device.id} has auto-import disabled")
            return
        }
        startImportForDevice(device, newFiles)
    }

    fun getStorageLocationForImport(device: RemovableDevice): File? {
        val mediaRoot = File(configService.getConfig().mediaPath).ensureWritable(
            eventPublisher, "ImportService-mediaRoot"
        ) ?: return null

        return File(mediaRoot, device.id).ensureWritable(
            eventPublisher, "ImportService-deviceFolder-${device.id}"
        ) ?: run {
            log.error("Device ${device.id} has no storage location")
            null
        }
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

    private val importQueues = ConcurrentHashMap<String, ConcurrentLinkedQueue<PendingFile>>()
    private val importTotals = ConcurrentHashMap<String, Int>()
    private val importedSuccessCount = ConcurrentHashMap<String, AtomicInteger>()
    private val scheduledTasks = ConcurrentHashMap<String, ScheduledFuture<*>>()

    fun startImportForDevice(device: RemovableDevice, files: List<KFile>) {
        val storage = getStorageLocationForImport(device) ?: return
        val deviceId = getDeviceIdToImport(device) ?: return
        val provider = providerFactory.getProvider(device)
        importTotals[device.id] = files.size
        importedSuccessCount[device.id] = AtomicInteger(0)
        // 1. Start en scheduler for denne enheten (hvis den ikke går)
        startDatabaseFlusher(device.id, deviceId)

        // 2. Start selve kopieringen (bør kjøre i egen tråd/Coroutine)
        Thread {
            files.forEach { file ->
                val imported = try {
                    provider.copyFile(device, storage, file)
                } catch (e: GPhoto2.CopyException) {
                    log.error("Failed to copy file {}", file, e)
                    null
                } catch (e: Exception) {
                    log.error("An unexpected error occurred while importing file {}", file, e)
                    null
                }
                if (imported != null) {
                    // Legg rett i køen med en gang filen er på disk
                    importQueues.getOrPut(device.id) { ConcurrentLinkedQueue() }
                        .add(PendingFile(deviceId, imported, ZonedDateTime.now()))
                }
            }
        }.start()
    }

    private fun startDatabaseFlusher(deviceIdStr: String, dbId: Int) {
        taskScheduler.scheduleAtFixedRate({
            val queue = importQueues[deviceIdStr] ?: return@scheduleAtFixedRate
            val totalExpected = importTotals[deviceIdStr] ?: 0

            val batch = mutableListOf<Pair<File, ZonedDateTime>>()
            var failedInThisBatch = 0

            while (queue.isNotEmpty()) {
                val pending = queue.poll() ?: break
                if (pending.file != null) {
                    batch.add(pending.file to pending.timestamp)
                    importedSuccessCount[deviceIdStr]?.incrementAndGet()
                } else {
                    failedInThisBatch++
                }
            }

            if (batch.isNotEmpty()) {
                fileRepository.saveFiles(dbId, batch)
            }

            // Sjekk om vi er ferdig (køen er tom, og vi har behandlet alle filer)
            val currentCount = importedSuccessCount[deviceIdStr]?.get() ?: 0
            if (currentCount + failedInThisBatch >= totalExpected) {
                finishImport(deviceIdStr, currentCount, failedInThisBatch)
            }
        }, Duration.ofSeconds(5))
    }

    private fun finishImport(deviceIdStr: String, successCount: Int, failCount: Int) {
        // 1. Rydd opp i scheduleren slik at den slutter å kjøre hvert 5. sekund
        scheduledTasks[deviceIdStr]?.cancel(false)
        scheduledTasks.remove(deviceIdStr)

        // 2. Rydd opp i map-ene (viktig for å unngå minnelekkasjer!)
        importQueues.remove(deviceIdStr)
        importTotals.remove(deviceIdStr)
        importedSuccessCount.remove(deviceIdStr)

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

    data class PendingFile(val deviceId: Int, val file: File, val timestamp: ZonedDateTime)
}