package no.iktdev.kammich.upload

import no.iktdev.kammich.immich.context.ImmichUserContext
import no.iktdev.kammich.models.internal.events.ImportJobClaimedEvent
import no.iktdev.kammich.upload.engine.IUploadEngine
import no.iktdev.kammich.upload.model.UploadJob
import no.iktdev.kammich.upload.model.UploadJobItem
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.*

@Service
class UploadingService(
    private val uploadStore: IUploadStore,
    private val uploadEngine: IUploadEngine,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun onImportJobClaimed(event: ImportJobClaimedEvent) {
        log.info("${event.jobId} claimed")

        val fileIds = uploadStore.getFileIdsAvailableForUpload(event.jobId)
        if (fileIds.isEmpty()) return

        val items = uploadStore.insertUploads(event.userId, event.jobId, fileIds)

        uploadEngine.submitJob(
            UploadJob(
                userId = event.userId,
                jobId = event.jobId,
                items = items
            )
        )
    }


    fun startUploadFor(userId: UUID) {
        uploadStore.getPendingUploadsOnUserId(userId)
            .groupBy { it.uploadJobId }
            .forEach { (jobId, uploads) ->
                if (jobId == null) return@forEach

                createJob(
                    userId = userId,
                    jobId = jobId,
                    items = uploads.map {
                        UploadJobItem(
                            uploadId = it.id,
                            fileId = it.importedFileId,
                            upload = it.state,
                            verify = it.verified
                        )
                    }
                )
            }
    }

    fun startUploadFor(userId: UUID, jobId: UUID) {
        createJob(
            userId = userId,
            jobId = jobId,
            items = uploadStore.getUploadJobItems(jobId)
        )
    }

    fun uploadSingleFile(fileId: Long, userId: UUID) {
        val jobId = UUID.randomUUID()

        createJob(
            userId = userId,
            jobId = jobId,
            items = uploadStore.insertUploads(userId, jobId, listOf(fileId))
        )
    }

    private fun createJob(userId: UUID, jobId: UUID, items: List<UploadJobItem>) {
        if (items.isEmpty()) return

        uploadEngine.submitJob(
            UploadJob(
                userId = userId,
                jobId = jobId,
                items = items
            )
        )
    }
}