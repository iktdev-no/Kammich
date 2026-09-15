package no.iktdev.kammich.upload.engine

import no.iktdev.kammich.getExifTimestamp
import no.iktdev.kammich.immich.IImmichApiClient
import no.iktdev.kammich.models.internal.immich.UploadAssetRequest
import no.iktdev.kammich.models.shared.UploadState
import no.iktdev.kammich.repository.FileResolver
import no.iktdev.kammich.upload.IUploadStore
import no.iktdev.kammich.upload.model.UploadFailed
import no.iktdev.kammich.upload.model.UploadJobItem
import no.iktdev.kammich.upload.model.UploadResult
import no.iktdev.kammich.upload.model.UploadSuccess
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.time.Instant

@Component
class Uploader(
    private val uploadStore: IUploadStore,
    private val fileResolver: FileResolver,
): IUploading {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun run(client: IImmichApiClient, job: UploadJobItem): UploadResult {
        val file = fileResolver.resolve(job.fileId)
            ?: return UploadFailed("File not found: ${job.fileId}")

        return try {
            val assetId = client.uploadFile(
                upload = UploadAssetRequest(
                    file = file,
                    modifiedAt = resolveModifiedAt(file),
                    createdAt = resolveCreatedAt(file) ?: resolveModifiedAt(file)
                )
            )

            if (assetId == null) {
                val reason = "No AssetId received"
                uploadStore.setUploadState(job.uploadId, null, UploadState.Failure, reason)
                UploadFailed(reason)
            } else {
                uploadStore.setUploadState(job.uploadId, assetId, UploadState.Success)
                UploadSuccess(assetId)
            }
        } catch (e: Exception) {
            log.error("Failed to upload ${file.absolutePath}", e)
            uploadStore.setUploadState(job.uploadId, null, UploadState.Failure, e.message)
            UploadFailed(e.message ?: "Unknown upload error")
        }
    }


    override fun resolveCreatedAt(file: File): Instant? =
        file.getExifTimestamp()

    override fun resolveModifiedAt(file: File): Instant =
        Instant.ofEpochMilli(file.lastModified())
}

interface IUploading {
    suspend fun run(client: IImmichApiClient, job: UploadJobItem): UploadResult
    fun resolveModifiedAt(file: File): Instant?
    fun resolveCreatedAt(file: File): Instant?
}