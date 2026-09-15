package no.iktdev.kammich.upload.engine

import kotlinx.coroutines.*
import no.iktdev.kammich.immich.IImmichApiClient
import no.iktdev.kammich.immich.services.ImmichContextService
import no.iktdev.kammich.models.internal.events.UploadCompletedEvent
import no.iktdev.kammich.models.internal.events.UploadedAssets
import no.iktdev.kammich.models.shared.*
import no.iktdev.kammich.repository.FileResolver
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.SSEUploadProgress
import no.iktdev.kammich.upload.IUploadStore
import no.iktdev.kammich.upload.model.*
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException

@Service
class UploadEngine(
    private val uploadStore: IUploadStore,
    private val queue: Queue,
    private val uploader: Uploader,
    private val verifier: Verifier,
    private val contextService: ImmichContextService,
    private val fileResolver: FileResolver,
    private val sseManager: SseManager,
    private val eventPublisher: ApplicationEventPublisher,
): IUploadEngine {
    private val log = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val currentTask = AtomicReference<Job?>()
    private val currentJob = AtomicReference<UploadJob?>()

    override fun submitJob(job: UploadJob) {
        queue.submit(job)
        updateProgress(job)
        runNext()
    }

    private fun runNext() {
        if (currentJob.get() != null) return

        val job = queue.next() ?: return
        if (!currentJob.compareAndSet(null, job)) {
            queue.submit(job)
            return
        }

        currentTask.set(scope.launch {
            try {
                run(job)
            }  catch (e: CancellationException) {
                job.apply { status = UploadJobStatus.Stopped }
                    .also { updateProgress(it) }
            }  catch (e: Exception) {
                log.error("Error running upload job ${job.jobId}", e)
                job.apply { status = UploadJobStatus.Failed }
                    .also { updateProgress(it) }
            } finally {
                currentJob.set(null)
                currentTask.set(null)
                runNext()
            }
        })
    }

    private suspend fun run(job: UploadJob) {
        job.apply { status = UploadJobStatus.Running }
            .also { updateProgress(it) }
        val client = contextService.getAKClient(job.userId) ?: throw IllegalStateException("Unable to create client for job ${job.jobId} on user ${job.userId}")
        job.items.forEach { item ->
            runUpload(item, client) {
                updateProgress(job)
            }
            runVerify(item, client) {
                updateProgress(job)
            }
        }
        job.apply { status = UploadJobStatus.Completed }
            .also {
                updateProgress(it)
                publishCompleted(it)
            }
    }

    private suspend fun runUpload(item: UploadJobItem, client: IImmichApiClient, onProgress: () -> Unit) {
        if (item.upload !in listOf(UploadState.Pending, UploadState.Uploading)) return

        item.upload = UploadState.Uploading
        onProgress()

        when (uploader.run(client, item)) {
            is UploadSuccess -> item.upload = UploadState.Success
            is UploadFailed -> item.upload = UploadState.Failure
        }
        onProgress()
    }

    private suspend fun runVerify(item: UploadJobItem, client: IImmichApiClient, onProgress: () -> Unit) {
        if (item.verify in listOf(Verification.Failed, Verification.Verified)) return // Silent ignore
        if (item.upload != UploadState.Success) {
            log.warn("Skipping verification for entry with fileId: ${item.fileId} and uploadId ${item.uploadId} due to non successful state")
            return
        }
        item.verify = Verification.Verifying
        onProgress()
        when (verifier.run(client, item)) {
            is VerifiedSuccess -> item.verify = Verification.Verified
            is VerificationFailed -> item.verify = Verification.Failed
            is VerificationSkipped -> item.verify = Verification.NotVerified
        }
        onProgress()
    }


    override fun stopJob(jobId: UUID): Boolean {
        val current = currentJob.get()

        if (current?.jobId == jobId) {
            currentTask.get()?.cancel() ?: return false
            return true
        }

        return queue.removeById(jobId)
    }

    override fun removeJob(jobId: UUID): Boolean =
        queue.removeById(jobId)

    override fun cancelAll() {
        currentJob.get()?.let {
            stopJob(it.jobId)
        }

        queue.clear()
    }

    override fun updateProgress(job: UploadJob) {
        val items = job.items.mapNotNull { item ->
            val file = fileResolver.resolve(item.fileId) ?: return@mapNotNull null

            UploadMediaItem(
                fileName = file.name,
                fileSize = file.length(),
                upload = item.upload,
                verification = item.verify
            )
        }

        val event = UploadProgressEvent(
            jobId = job.jobId,
            totalFiles = items.size,
            failedFiles = items.count {
                it.upload == UploadState.Failure || it.verification == Verification.Failed
            },
            successfulFiles = items.count {
                it.upload == UploadState.Success && it.verification == Verification.Verified
            },
            items = items,
            state = job.status
        )

        sseManager.send(SSEUploadProgress(event))
    }

    override fun isRunning(jobId: UUID): Boolean =
        currentJob.get()?.jobId == jobId

    private fun publishCompleted(job: UploadJob) {
        val completed = job.items.filter {
            it.upload == UploadState.Success && it.verify == Verification.Verified
        }

        val resolvedFiles = fileResolver.resolve(completed.map { it.fileId })
        val assetIds = uploadStore.getAssetIdOnUploadIds(completed.map { it.uploadId })

        val uploads = completed.mapNotNull { item ->
            val file = resolvedFiles[item.fileId] ?: return@mapNotNull null
            val assetId = assetIds[item.uploadId] ?: return@mapNotNull null

            UploadedAssets(
                uploadedId = item.uploadId,
                assetId = assetId,
                absolutePath = file.absolutePath
            )
        }

        eventPublisher.publishEvent(UploadCompletedEvent(job.userId, uploads))
    }
}

interface IUploadEngine {
    fun submitJob(job: UploadJob)
    fun removeJob(jobId: UUID): Boolean
    fun stopJob(jobId: UUID): Boolean
    fun cancelAll()
    fun isRunning(jobId: UUID): Boolean
    fun updateProgress(job: UploadJob)
}