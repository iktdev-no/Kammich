package no.iktdev.kammich.services.upload

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory
import no.iktdev.kammich.database.tables.UploadFilesTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.getExifTimestamp
import no.iktdev.kammich.immich.ImmichApi
import no.iktdev.kammich.immich.client.ImmichClientFactory
import no.iktdev.kammich.immich.services.ImmichContextService
import no.iktdev.kammich.models.internal.immich.UploadAssetRequest
import no.iktdev.kammich.models.shared.JobStatus
import no.iktdev.kammich.models.shared.UploadMediaItem
import no.iktdev.kammich.models.shared.UploadState
import no.iktdev.kammich.models.shared.UploadProgressEvent
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.SSEUploadProgress
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.time.Instant
import java.util.UUID

data class UploadJob(
    val userId: UUID,
    val jobId: UUID,
    val items: List<UploadJobItem>,
) {}

data class UploadJobItem(
    val uploadId: Long,
    val fileId: Long,
    val absolutePath: String,
    var uploadState: UploadState = UploadState.Pending,
)

sealed class UploadResult()
data class UploadSuccess(val assetId: UUID): UploadResult()
data class UploadFailed(val reason: String): UploadResult()

sealed class UploadJobResult(open val job: UploadJob)
data class UploadJobSuccess(override val job: UploadJob): UploadJobResult(job)
data class UploadJobFailed(override val job: UploadJob, val reason: String): UploadJobResult(job)

@Component
class Uploader(
    private val immichClientFactory: ImmichClientFactory,
    private val sseManager: SseManager,
    private val contextService: ImmichContextService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private fun ImmichContextService.SavedSession.toClient(): ImmichApi {
        return immichClientFactory.create(this.serverUrl)
    }

    private suspend fun ImmichApi.performUpload(apiKey: String, file: File): UploadResult {
        val timestamp = Instant.ofEpochMilli(file.lastModified())
        return try {
            val res = this.uploadFile(apiKey, UploadAssetRequest(
                file = file,
                modifiedAt = timestamp,
                createdAt = file.getExifTimestamp() ?: timestamp,
            ))
            if (res != null) UploadSuccess(res) else {
                log.error("Could get assetId for upload file ${file.absolutePath}")
                UploadFailed("No AssetId received!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UploadFailed(e.message ?: e.localizedMessage)
        }
    }

    private fun updateUploadTable(uploadId: Long, assetId: UUID?, state: UploadState) {
        withTransaction {
            UploadFilesTable.update({ UploadFilesTable.id eq uploadId }) {
                it[UploadFilesTable.state] = state
                it[UploadFilesTable.immichAssetId] = assetId?.toString()
                it[UploadFilesTable.updatedAt] = Instant.now().toString()
            }
        }
    }

    suspend fun startUploadJob(job: UploadJob): UploadJobResult {
        val session = contextService.findSessionsByUserId(job.userId) ?: run {
            updateProgress(JobStatus.Failed, job)
            return UploadJobFailed(job, "Could not find sessions for ${job.userId}")
        }
        try {
            job.items.forEach { item ->
                val result = session.toClient().performUpload(session.apiKey, File(item.absolutePath))
                if (result is UploadSuccess) {
                    updateUploadTable(item.uploadId, result.assetId, UploadState.Success)
                    item.uploadState = UploadState.Success
                } else {
                    updateUploadTable(item.uploadId, null, UploadState.Failure)
                    item.uploadState = UploadState.Failure
                }
                updateProgress(JobStatus.Running, job)
            }
        } catch (e: Exception) {
            log.error("Failed to perform upload", e)
            e.printStackTrace()
            updateProgress(JobStatus.Failed, job)
            return UploadJobFailed(job, e.message ?: e.localizedMessage)
        }
        updateProgress(JobStatus.Completed, job)
        return UploadJobSuccess(job)
    }

    private fun updateProgress(status: JobStatus, job: UploadJob) {
        val items = job.items.map { it ->
            val file = File(it.absolutePath)
            UploadMediaItem(
                fileName = file.name,
                fileSize = file.length(),
                state = it.uploadState
            )
        }
        val uploadStateEvent = UploadProgressEvent(
            jobId = job.jobId,
            totalFiles = items.size,
            failedFiles = items.count { it.state == UploadState.Failure },
            successfulFiles = items.count { it.state == UploadState.Success },
            state = status,
            items = items
        )
        sseManager.send(SSEUploadProgress(uploadStateEvent))
    }


}