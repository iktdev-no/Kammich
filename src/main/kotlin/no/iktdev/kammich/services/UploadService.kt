package no.iktdev.kammich.services


import no.iktdev.kammich.models.shared.upload.UploadJobSummary
import no.iktdev.kammich.models.shared.upload.UploadSummary
import no.iktdev.kammich.upload.IUploadStore
import no.iktdev.kammich.upload.UploadingService
import no.iktdev.kammich.upload.engine.IUploadEngine
import org.jetbrains.exposed.v1.core.*
import org.springframework.stereotype.Service
import java.util.*

@Service
class UploadService(
    private val uploadStore: IUploadStore,
    private val uploadingService: UploadingService,
    private val uploadEngine: IUploadEngine,
) {

    fun getCheckForRemainingFiles(userId: UUID) {
        uploadStore.getUploadsOnUserId(userId)
    }

    fun resetFailedUploadsByUser(userId: UUID): Map<UUID, Boolean> =
        uploadStore.getUploadJobIdsOnUserId(userId)
            .associateWith { jobId ->
                resetFailedUploadJob(userId, jobId)
            }

    fun resetFailedUploadJob(userId: UUID, jobId: UUID): Boolean {
        val reset = uploadStore.resetFailedUploadsOnJobId(jobId)
        if (!reset) return false

        uploadingService.startUploadFor(userId, jobId)
        return true
    }

    fun getUploadSummary(userId: UUID): UploadSummary =
        uploadStore.getUploadSummary(userId)

    fun getJobUploadSummaries(userId: UUID): List<UploadJobSummary> =
        uploadStore.getJobUploadSummaries(userId)
            .map { it.copy(isRunning = uploadEngine.isRunning(it.jobId)) }

    fun uploadFile(userId: UUID, fileId: Long) =
        uploadingService.uploadSingleFile(fileId, userId)

    fun startUpload(userId: UUID, jobId: UUID) =
        uploadingService.startUploadFor(userId, jobId)
}
