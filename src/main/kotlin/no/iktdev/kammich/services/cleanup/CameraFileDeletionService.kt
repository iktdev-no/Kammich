package no.iktdev.kammich.services.cleanup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import no.iktdev.kammich.database.tables.DeleteFilesTable
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.ImportedFilesTable
import no.iktdev.kammich.database.tables.UploadFilesTable
import no.iktdev.kammich.database.tables.getDeviceId
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.errorNotification
import no.iktdev.kammich.infoNotification
import no.iktdev.kammich.models.internal.events.ImportJobCompletedEvent
import no.iktdev.kammich.models.shared.Verification
import no.iktdev.kammich.models.shared.deletion.DeleteState
import no.iktdev.kammich.storage.DeviceManagerService
import no.iktdev.kammich.storage.provider.DeviceUnavailableException
import no.iktdev.kammich.storage.provider.StorageProviderFactory
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CameraFileDeletionService(
    private val deviceManager: DeviceManagerService,
    private val providerFactory: StorageProviderFactory,
    private val eventPublisher: ApplicationEventPublisher
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @EventListener
    fun onImportJobCompleted(event: ImportJobCompletedEvent) {
        serviceScope.launch {
            cleanupCamera(event.deviceSN)
        }
    }

    private suspend fun cleanupCamera(deviceSN: String) {
        val device = deviceManager.getDevice(deviceSN) ?: run {
            eventPublisher.errorNotification(
                "FileDeletionService-DeviceMissing-$deviceSN",
                "Kamera ikke funnet",
                "Fant ikke kameraet $deviceSN under opprydding."
            )
            return
        }
        val allowDeletion = deviceManager.getSettings(deviceSN).autoImport ?: false
        if (!allowDeletion) {
            return
        }

        val provider = providerFactory.getProvider(device)

        val dcim = provider.getDCIM(device) ?: run {
            eventPublisher.errorNotification(
                "FileDeletionService-NoDCIM-$deviceSN",
                "Fant ikke DCIM",
                "Fant ikke DCIM-mappen på kameraet $deviceSN."
            )
            return
        }

        val files = provider.listAllFiles(device, dcim.path)
        val pending = getPendingCameraDeletes(deviceSN)

        log.info(
            "Starting camera cleanup for {}. Found {} files on camera, {} pending deletions",
            deviceSN,
            files.size,
            pending.size
        )

        if (pending.isEmpty()) {
            eventPublisher.infoNotification(
                "FileDeletionService-NoFiles-$deviceSN",
                "Kameraopprydding ferdig",
                "Ingen filer ventet på sletting fra kameraet $deviceSN."
            )
            return
        }

        var deleted = 0
        var failed = 0

        files.forEach { file ->
            val deleteId = pending[file.path] ?: return@forEach

            try {
                if (provider.deleteFile(device, file)) {
                    markCameraDeleted(deleteId)
                    deleted++
                } else {
                    markCameraDeleteFailed(deleteId, "Could not delete file")
                    failed++
                }
            } catch (e: DeviceUnavailableException) {
                log.warn("Device {} became unavailable during cleanup", deviceSN)
                eventPublisher.errorNotification(
                    "FileDeletionService-Disconnected-$deviceSN",
                    "Kamera frakoblet",
                    "Kameraet $deviceSN ble frakoblet under sletting."
                )
                return
            } catch (e: Exception) {
                failed++
                markCameraDeleteFailed(
                    deleteId,
                    e.message ?: "Unknown error"
                )
                log.error("Failed to delete camera file {}", file.path, e)
            }
        }

        val remaining = pending.size - deleted - failed

        if (failed > 0 || remaining > 0) {
            eventPublisher.errorNotification(
                "FileDeletionService-Failed-$deviceSN",
                "Kameraopprydding feilet",
                "Slettet $deleted av ${pending.size} filer fra kameraet ${device.model}. " +
                        "$failed feilet og $remaining gjenstår."
            )
        } else {
            eventPublisher.infoNotification(
                "FileDeletionService-Completed-$deviceSN",
                "Kameraopprydding ferdig",
                "Slettet $deleted filer fra kameraet ${device.model}."
            )
        }
    }

    private fun getPendingCameraDeletes(deviceId: String): Map<String, Long> {
        val databaseDeviceId = DevicesTable.getDeviceId(deviceId) ?: return emptyMap()

        return withTransaction {
            DeleteFilesTable
                .innerJoin(ImportedFilesTable)
                .innerJoin(UploadFilesTable)
                .select(
                    DeleteFilesTable.id,
                    ImportedFilesTable.cameraPath
                )
                .where {
                    (ImportedFilesTable.deviceId eq databaseDeviceId)
                        .and(UploadFilesTable.verified eq Verification.Verified)
                        .and(DeleteFilesTable.cameraState neq DeleteState.Deleted)
                }
                .associate {
                    it[ImportedFilesTable.cameraPath] to it[DeleteFilesTable.id].value
                }
        }.getOrThrow()
    }

    private fun markCameraDeleted(deleteId: Long) {
        withTransaction {
            DeleteFilesTable.update({ DeleteFilesTable.id eq deleteId }) {
                it[cameraState] = DeleteState.Deleted
                it[cameraDeletedAt] = Instant.now().toString()
                it[cameraErrorMessage] = null
                it[updatedAt] = Instant.now().toString()
            }
        }
    }

    private fun markCameraDeleteFailed(deleteId: Long, message: String) {
        withTransaction {
            DeleteFilesTable.update({ DeleteFilesTable.id eq deleteId }) {
                it[cameraState] = DeleteState.Failed
                it[cameraErrorMessage] = message
                it[retryCount] = DeleteFilesTable.retryCount + 1
                it[updatedAt] = Instant.now().toString()
            }
        }
    }
}