package no.iktdev.kammich.services.cleanup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import no.iktdev.kammich.database.tables.DeleteFilesTable
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.ImportedFilesTable
import no.iktdev.kammich.database.tables.UploadFilesTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.models.internal.events.ImportJobCompletedEvent
import no.iktdev.kammich.models.shared.Verification
import no.iktdev.kammich.models.shared.deletion.DeleteState
import no.iktdev.kammich.services.ConfigService
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class LocalFileDeletionService(
    private val configService: ConfigService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val serviceScope =
        CoroutineScope(Dispatchers.IO + SupervisorJob())

    @EventListener
    fun onImportJobCompleted(event: ImportJobCompletedEvent) {
        serviceScope.launch {
            cleanup()
        }
    }

    private fun cleanup() {
        val files = getFilesReadyToDelete()

        files.forEach { file ->
            try {
                log.info("Attempting to delete local file {}", file.file)

                if (!file.file.exists()) {
                    log.info("Local file already gone: {}", file.file)
                    markDeleted(file.id)
                    return@forEach
                }

                if (file.file.delete()) {
                    log.info("Deleted local file {}", file.file)
                    markDeleted(file.id)
                } else {
                    markFailed(file.id, "Could not delete file")
                }
            } catch (e: Exception) {
                log.error("Failed to delete local file {}", file.file, e)
                markFailed(file.id, e.message ?: "Unknown error")
            }
        }
    }

    private fun getFilesReadyToDelete(): List<DeleteFileItem> {
        val mediaPath = configService.getConfig().mediaPath
        val cutoff = Instant.now().minus(1, ChronoUnit.DAYS)

        return withTransaction {
            DeleteFilesTable
                .innerJoin(ImportedFilesTable)
                .innerJoin(UploadFilesTable)
                .innerJoin(DevicesTable)
                .select(
                    DeleteFilesTable.id,
                    ImportedFilesTable.fileName,
                    DevicesTable.serialNumber
                )
                .where {
                    (UploadFilesTable.verified eq Verification.Verified)
                        .and(DeleteFilesTable.localState neq DeleteState.Deleted)
                        .and(ImportedFilesTable.importedAt lessEq cutoff.toString())
                }
                .map {
                    DeleteFileItem(
                        id = it[DeleteFilesTable.id].value,
                        file = File(
                            mediaPath,
                            "${it[DevicesTable.serialNumber]}/${it[ImportedFilesTable.fileName]}"
                        )
                    )
                }
        }.getOrThrow()
    }

    private fun markDeleted(id: Long) {
        withTransaction {
            DeleteFilesTable.update({
                DeleteFilesTable.id eq id
            }) {
                it[localState] = DeleteState.Deleted
                it[localDeletedAt] = Instant.now().toString()
                it[localErrorMessage] = null
                it[updatedAt] = Instant.now().toString()
            }
        }
    }

    private fun markFailed(id: Long, message: String) {
        withTransaction {
            DeleteFilesTable.update({
                DeleteFilesTable.id eq id
            }) {
                it[localState] = DeleteState.Failed
                it[localErrorMessage] = message
                it[retryCount] = DeleteFilesTable.retryCount + 1
                it[updatedAt] = Instant.now().toString()
            }
        }
    }

    private data class DeleteFileItem(
        val id: Long,
        val file: File
    )
}