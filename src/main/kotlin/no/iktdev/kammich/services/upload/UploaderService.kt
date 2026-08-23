package no.iktdev.kammich.services.upload

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.ImportedFilesTable
import no.iktdev.kammich.database.tables.UploadFilesTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.models.internal.events.ImportJobClaimedEvent
import no.iktdev.kammich.models.internal.events.UploadCompletedEvent
import no.iktdev.kammich.models.internal.events.UploadedAssets
import no.iktdev.kammich.models.shared.UploadState
import no.iktdev.kammich.services.ConfigService
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.io.File
import java.time.Instant
import java.util.UUID

@Service
class UploaderService(
    private val configService: ConfigService,
    private val uploader: Uploader,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun getFileWithId(fileIds: List<Long>): Map<Long, File> {
        val mediaPath = configService.getConfig().mediaPath
        val fileList = withTransaction {
            ImportedFilesTable.getWhere { ImportedFilesTable.id inList fileIds }
        }.getOrThrow()
        if (fileList.isEmpty()) {
            log.error("No files found to be uploaded!");
            return emptyMap()
        }
        val relevantDevices = withTransaction {
            val deviceIds = fileList.map { it.deviceId }
            DevicesTable.select(DevicesTable.id, DevicesTable.serialNumber)
                .where { DevicesTable.id inList deviceIds }
                .associate { row ->
                    row[DevicesTable.id].value to row[DevicesTable.serialNumber]
                }
        }.getOrThrow()

        return fileList.associate { file ->
            val serialNumber = relevantDevices[file.deviceId]
                ?: throw IllegalStateException("Serial number not found for device ID: ${file.deviceId}")
            file.id to file.getFile(mediaPath, serialNumber)
        }
    }

    @EventListener
    fun onImportJobClaimed(event: ImportJobClaimedEvent) {
        log.info("Incoming JobClaimedEvent")
        val importIds = withTransaction {
            ImportedFilesTable.getWhere { ImportedFilesTable.importJob eq event.jobId.toString() }
        }.getOrThrow().map { it -> it.id }

        log.error("No imported files found for job ID: ${event.jobId}")

        val filtered = withTransaction {
            UploadFilesTable.select(UploadFilesTable.importedFileId)
                .where(UploadFilesTable.uploadJobId eq event.jobId.toString())
                .map { it[UploadFilesTable.importedFileId].value }
        }.getOrDefault(emptyList())

        val usableImportedIds = importIds.filter { it !in filtered }
        val filing = getFileWithId(usableImportedIds)

        insertInitialStateAndUpload(event.userId, event.jobId, filing)
    }

    fun uploadSingleFile(fileId: Long, userId: UUID) {
        val filing = getFileWithId(fileIds = listOf(fileId))
        insertInitialStateAndUpload(userId, UUID.randomUUID(), filing)
    }

    private fun insertInitialStateAndUpload(userId: UUID, jobId: UUID, filing: Map<Long, File>) {
        log.info("Incoming upload on $jobId")
        serviceScope.launch {
            val uploadItems = withTransaction {
                filing.map { (fileId, file) ->
                    val uploadId = UploadFilesTable.insertAndGetId {
                        it[importedFileId] = fileId
                        it[uploadJobId] = jobId.toString()
                        it[immichUserId] = userId.toString()
                        it[updatedAt] = Instant.now().toString()
                    }.value
                    UploadJobItem(
                        uploadId = uploadId,
                        fileId = fileId,
                        absolutePath = file.absolutePath,
                    )
                }
            }.getOrThrow()
            if (uploadItems.isEmpty()) {
                return@launch
            }
            performUpload(userId, jobId, uploadItems)
        }
    }

    fun uploadAlreadyExistingUploadEntries(userId: UUID, jobId: UUID, importedFileIds: List<Long>) {
        val filing = getFileWithId(fileIds = importedFileIds)

        serviceScope.launch {
            val uploadItems = withTransaction {
                val existingRows = UploadFilesTable.getWhere {
                    (UploadFilesTable.uploadJobId eq jobId.toString()) and
                            (UploadFilesTable.importedFileId inList importedFileIds)
                }
                existingRows.mapNotNull {
                    val fi = filing[it.importedFileId] ?: return@mapNotNull null
                    UploadJobItem(
                        uploadId = it.id,
                        fileId = it.importedFileId,
                        absolutePath = fi.absolutePath,
                    )
                }
            }.getOrThrow()
            if (uploadItems.isEmpty()) {
                return@launch
            }
            val job = UploadJob(userId, jobId, uploadItems)
            val result = uploader.startUploadJob(job)

            val complete = getUploadCompleted(userId, result.job.items)
            eventPublisher.publishEvent(complete)
        }
    }

    private suspend fun performUpload(userId: UUID, jobId: UUID, uploadItems: List<UploadJobItem>) {
        val job = UploadJob(userId, jobId, uploadItems)
        val result = uploader.startUploadJob(job)

        val complete = getUploadCompleted(userId, result.job.items)
        eventPublisher.publishEvent(complete)
    }

private fun getUploadCompleted(userId: UUID, items: List<UploadJobItem>): UploadCompletedEvent {
    val workItemMap = items.associateBy { it.uploadId }
    val uploadIds = workItemMap.keys.toList()

    val readyFiles = withTransaction {
        UploadFilesTable.getWhere {
            (UploadFilesTable.id inList uploadIds) and (UploadFilesTable.state eq UploadState.Success)
        }
    }.getOrDefault(emptyList())

    val uploads = readyFiles.mapNotNull { dbRow ->
        val file = workItemMap[dbRow.id]?.absolutePath ?: return@mapNotNull null
        val assetId = dbRow.immichAssetId ?: return@mapNotNull null

        UploadedAssets(
            uploadedId = dbRow.id,
            assetId = assetId,
            absolutePath = file
        )
    }

    return UploadCompletedEvent(userId, uploads)
}




}