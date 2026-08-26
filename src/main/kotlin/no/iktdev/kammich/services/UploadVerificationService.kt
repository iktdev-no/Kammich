package no.iktdev.kammich.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import no.iktdev.kammich.database.tables.DeleteFilesTable
import no.iktdev.kammich.database.tables.ImportedFilesTable
import no.iktdev.kammich.database.tables.UploadFilesTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.immich.ImmichApi
import no.iktdev.kammich.immich.client.ImmichClientFactory
import no.iktdev.kammich.immich.services.ImmichContextService
import no.iktdev.kammich.models.FileHashType
import no.iktdev.kammich.models.internal.events.UploadCompletedEvent
import no.iktdev.kammich.models.shared.UploadState
import no.iktdev.kammich.models.shared.Verification
import no.iktdev.kammich.sse.SseManager
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Service
class UploadVerificationService(
    private val immichClientFactory: ImmichClientFactory,
    private val sseManager: SseManager,
    private val contextService: ImmichContextService
) {
    data class UploadVerificationItem(
        val userId: UUID,
        val uploadId: Long,
        val importedFileId: Long,
        val assetId: UUID,
        val localChecksum: String
    )


    private val log = LoggerFactory.getLogger(javaClass)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun ImmichContextService.SavedSession.toClient(): ImmichApi {
        return immichClientFactory.create(this.serverUrl)
    }

    @EventListener
    fun onUploadCompleted(event: UploadCompletedEvent) {
        val toBeVerified = withTransaction {
            UploadFilesTable
                .innerJoin(ImportedFilesTable)
                .select(
                    UploadFilesTable.id,
                    UploadFilesTable.immichUserId,
                    UploadFilesTable.immichAssetId,
                    ImportedFilesTable.checksum,
                    ImportedFilesTable.id
                )
                .where {
                    (UploadFilesTable.state eq UploadState.Success) and
                            (UploadFilesTable.verified eq Verification.NotVerified) and
                            (ImportedFilesTable.checksumType eq FileHashType.SHA1.name)
                }
                .mapNotNull { row ->
                    val assetId = row[UploadFilesTable.immichAssetId]
                        ?: run {
                            log.error(
                                "Upload {} is successful but has no Immich asset ID",
                                row[UploadFilesTable.id].value
                            )
                            return@mapNotNull null
                        }

                    UploadVerificationItem(
                        userId = UUID.fromString(row[UploadFilesTable.immichUserId]),
                        uploadId = row[UploadFilesTable.id].value,
                        assetId = UUID.fromString(assetId),
                        localChecksum = row[ImportedFilesTable.checksum],
                        importedFileId = row[ImportedFilesTable.id].value
                    )
                }
        }.getOrThrow()

        toBeVerified
            .groupBy { it.userId }
            .forEach { (userId, files) ->
                val session = contextService.findSessionsByUserId(userId) ?: run {
                    log.error("Could not find session for user {}", userId)
                    return@forEach
                }

                serviceScope.launch {
                    verifyUploads(session, files)
                }
            }
    }

    private fun base64ToHex(value: String): String =
        Base64.getDecoder()
            .decode(value)
            .joinToString("") { "%02x".format(it) }

    private suspend fun verifyUploads(
        session: ImmichContextService.SavedSession,
        files: List<UploadVerificationItem>
    ) {
        val client = session.toClient()

        files.forEach { file ->
            try {
                val asset = client.getFileInfo(
                    session.apiKey,
                    file.assetId
                )

                val immichChecksum = base64ToHex(asset.checksum)

                if (immichChecksum.equals(file.localChecksum, ignoreCase = true)) {
                    log.info("Verified upload {} / asset {}", file.uploadId, file.assetId)

                    updateVerification(file, Verification.Verified)
                } else {
                    log.error("Checksum mismatch for upload {} / asset {}. Local={}, Immich={}", file.uploadId, file.assetId, file.localChecksum, immichChecksum)
                    updateVerification(file, Verification.Failed)
                }
            } catch (e: Exception) {
                log.error("Failed to verify upload {} / asset {}", file.uploadId, file.assetId, e)
            }
        }
    }

    private fun updateVerification(upload: UploadVerificationItem, result: Verification) {
        withTransaction {
            UploadFilesTable.update({
                UploadFilesTable.id eq upload.uploadId
            }) {
                it[verified] = result
                it[updatedAt] = Instant.now().toString()
            }

            if (result == Verification.Verified) {
                DeleteFilesTable.insertIgnore {
                    it[importedFileId] = upload.importedFileId
                    it[uploadFileId] = upload.uploadId
                    it[createdAt] = Instant.now().toString()
                    it[updatedAt] = Instant.now().toString()
                }
            }
        }
    }




}