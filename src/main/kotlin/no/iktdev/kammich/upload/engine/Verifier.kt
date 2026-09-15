package no.iktdev.kammich.upload.engine

import no.iktdev.kammich.immich.IImmichApiClient
import no.iktdev.kammich.models.shared.UploadState
import no.iktdev.kammich.models.shared.Verification
import no.iktdev.kammich.repository.FileResolver
import no.iktdev.kammich.upload.IUploadStore
import no.iktdev.kammich.upload.model.UploadJobItem
import no.iktdev.kammich.upload.model.VerificationFailed
import no.iktdev.kammich.upload.model.VerificationResult
import no.iktdev.kammich.upload.model.VerificationSkipped
import no.iktdev.kammich.upload.model.VerifiedSuccess
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*


@Component
class Verifier(
    private val uploadStore: IUploadStore
): IVerifier {
    private val log = LoggerFactory.getLogger(javaClass)
    private val decoder = Base64.getDecoder()

    override suspend fun run(client: IImmichApiClient, job: UploadJobItem): VerificationResult {
        if (job.upload != UploadState.Success) return VerificationSkipped(job.uploadId)

        val assetId = uploadStore.getAssetIdOnUploadId(job.uploadId)
            ?: return VerificationFailed(job.uploadId, "No Immich asset ID for upload ${job.uploadId}")

        val localChecksum = uploadStore.getChecksumOnFileId(job.fileId)
            ?: return VerificationFailed(job.uploadId, "No checksum for file ${job.fileId}")

        return try {
            val immichChecksum = decoder.decode(client.getFileInfo(assetId).checksum)
                .joinToString("") { "%02x".format(it) }

            if (immichChecksum.equals(localChecksum, ignoreCase = true)) {
                log.info("Verified upload {} / asset {}", job.uploadId, assetId)
                uploadStore.setUploadVerification(job.uploadId, Verification.Verified)
                VerifiedSuccess(job.uploadId)
            } else {
                val error = "Checksum mismatch. Local=$localChecksum, Immich=$immichChecksum"
                log.error("Upload {} / asset {}: {}", job.uploadId, assetId, error)
                uploadStore.setUploadVerification(job.uploadId, Verification.Failed, error)
                VerificationFailed(job.uploadId, error)
            }
        } catch (e: Exception) {
            val error = e.message ?: "Unknown verification error"
            log.error("Failed to verify upload {} / asset {}", job.uploadId, assetId, e)
            uploadStore.setUploadVerification(job.uploadId, Verification.NotVerified, error)
            VerificationFailed(job.uploadId, error)
        }
    }
}

interface IVerifier {
    suspend fun run(client: IImmichApiClient, job: UploadJobItem): VerificationResult
}