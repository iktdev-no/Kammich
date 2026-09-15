package no.iktdev.kammich.upload

import no.iktdev.kammich.database.models.PersistedUploadFile
import no.iktdev.kammich.database.tables.ImportedFilesTable
import no.iktdev.kammich.database.tables.UploadFilesTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.models.shared.UploadState
import no.iktdev.kammich.models.shared.Verification
import no.iktdev.kammich.models.shared.upload.UploadJobSummary
import no.iktdev.kammich.models.shared.upload.UploadSummary
import no.iktdev.kammich.upload.model.DeletableFile
import no.iktdev.kammich.upload.model.UploadJobItem
import no.iktdev.kammich.upload.model.UploadRecord
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.insertIgnoreAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Component
import java.io.File
import java.time.Instant
import java.util.UUID

@Component
class UploadStore: IUploadStore {
    override fun getFileIdsAvailableForUpload(jobId: UUID): List<Long> {
        val alreadyInUploads = getFileIdsInUploadsByJobId(jobId).toSet()
        val filesInJob = withTransaction {
            ImportedFilesTable.getWhere { ImportedFilesTable.importJob eq jobId.toString() }
        }.getOrDefault(emptyList()).map { it.id }

        return filesInJob.filter { it !in alreadyInUploads }
    }

    override fun getFileIdsInUploadsByJobId(jobId: UUID): List<Long> =
        withTransaction {
            UploadFilesTable.select(UploadFilesTable.importedFileId)
                .where(UploadFilesTable.uploadJobId eq jobId.toString())
                .map { it[UploadFilesTable.importedFileId].value }
        }.getOrDefault(emptyList())

    override fun getPendingUploadsOnJobId(jobId: UUID): List<PersistedUploadFile> =
        withTransaction {
            UploadFilesTable.getWhere {
                (UploadFilesTable.uploadJobId eq jobId.toString())
                    .and(UploadFilesTable.state eq UploadState.Pending)
            }
        }.getOrDefault(emptyList())

    override fun getPendingUploadsOnUserId(userId: UUID): List<PersistedUploadFile> =
        withTransaction {
            UploadFilesTable.getWhere {
                (UploadFilesTable.immichUserId eq userId.toString())
                    .and(UploadFilesTable.state eq UploadState.Pending)
            }
        }.getOrDefault(emptyList())

    override fun getUnverifiedUploadsOnUserId(userId: UUID): List<PersistedUploadFile> =
        withTransaction {
            UploadFilesTable.getWhere {
                (UploadFilesTable.immichUserId eq userId.toString())
                .and(UploadFilesTable.state eq UploadState.Success)
                    .and(UploadFilesTable.verified neq Verification.Verified)
            }
        }.getOrDefault(emptyList())


    override fun getUnverifiedUploadsOnJobId(jobId: UUID): List<PersistedUploadFile> =
        withTransaction {
            UploadFilesTable.getWhere {
                (UploadFilesTable.uploadJobId eq jobId.toString())
                    .and(UploadFilesTable.state eq UploadState.Success)
                    .and(UploadFilesTable.verified neq Verification.Verified)
            }
        }.getOrDefault(emptyList())

    override fun getSuccessfulUploadsOnUserId(userId: UUID): List<PersistedUploadFile> =
        withTransaction {
            UploadFilesTable.getWhere {
                (UploadFilesTable.immichUserId eq userId.toString())
                    .and(UploadFilesTable.state eq UploadState.Success)
                    .and(UploadFilesTable.verified eq Verification.Verified)
                    .and { UploadFilesTable.immichAssetId.isNotNull() }
            }
        }.getOrDefault(emptyList())

    override fun getSuccessfulUploadsOnJobId(jobId: UUID): List<PersistedUploadFile> =
        withTransaction {
            UploadFilesTable.getWhere {
                (UploadFilesTable.uploadJobId eq jobId.toString())
                    .and(UploadFilesTable.state eq UploadState.Success)
                    .and(UploadFilesTable.verified eq Verification.Verified)
                    .and { UploadFilesTable.immichAssetId.isNotNull() }

            }
        }.getOrDefault(emptyList())

    override fun getAssetIdOnUploadId(uploadId: Long): UUID? =
        withTransaction {
            UploadFilesTable
                .select(UploadFilesTable.immichAssetId)
                .where { UploadFilesTable.id eq uploadId }
                .singleOrNull()
                ?.get(UploadFilesTable.immichAssetId)
                ?.let(UUID::fromString)
        }.getOrThrow()

    override fun getAssetIdOnUploadIds(uploadIds: List<Long>): Map<Long, UUID> =
        withTransaction {
            UploadFilesTable
                .select(UploadFilesTable.id, UploadFilesTable.immichAssetId)
                .where { UploadFilesTable.id inList uploadIds }
                .mapNotNull { row ->
                    row[UploadFilesTable.immichAssetId]?.let { assetId ->
                        row[UploadFilesTable.id].value to UUID.fromString(assetId)
                    }
                }
                .toMap()
        }.getOrThrow()

    override fun getChecksumOnFileId(fileId: Long): String? =
        withTransaction {
            ImportedFilesTable
                .select(ImportedFilesTable.checksum)
                .where { ImportedFilesTable.id eq fileId }
                .singleOrNull()
                ?.get(ImportedFilesTable.checksum)
        }.getOrThrow()

    override fun insertUploads(userId: UUID, jobId: UUID, fileIds: List<Long>): List<UploadJobItem> {
        withTransaction {
            fileIds.forEach { fileId ->
                UploadFilesTable.insertIgnoreAndGetId {
                    it[importedFileId] = fileId
                    it[uploadJobId] = jobId.toString()
                    it[immichUserId] = userId.toString()
                    it[updatedAt] = Instant.now().toString()
                }
            }
        }.getOrThrow()
        return getUploadJobItems(fileIds)
    }

    override fun getUploadJobItems(jobId: UUID): List<UploadJobItem> = withTransaction {
        UploadFilesTable
            .select(
                UploadFilesTable.id,
                UploadFilesTable.importedFileId,
                UploadFilesTable.state,
                UploadFilesTable.verified
            )
            .where { UploadFilesTable.uploadJobId eq jobId.toString() }
            .map { it.toUploadJobItem() }
    }.getOrThrow()

    override fun getUploadJobItems(fileIds: List<Long>): List<UploadJobItem> = withTransaction {
        UploadFilesTable
            .select(
                UploadFilesTable.id,
                UploadFilesTable.importedFileId,
                UploadFilesTable.state,
                UploadFilesTable.verified
            )
            .where { UploadFilesTable.importedFileId inList fileIds }
            .map { it.toUploadJobItem() }
    }.getOrThrow()

    private fun ResultRow.toUploadJobItem() = UploadJobItem(
        uploadId = this[UploadFilesTable.id].value,
        fileId = this[UploadFilesTable.importedFileId].value,
        upload = this[UploadFilesTable.state],
        verify = this[UploadFilesTable.verified]
    )

    override fun setUploadState(
        uploadId: Long,
        immichAssetId: UUID?,
        state: UploadState,
        error: String?
    ): Boolean =
        withTransaction {
            UploadFilesTable.update({ UploadFilesTable.id eq uploadId }) {
                it[UploadFilesTable.state] = state
                it[UploadFilesTable.errorMessage] = error
                it[UploadFilesTable.immichAssetId] = immichAssetId?.toString()
                it[UploadFilesTable.updatedAt] = Instant.now().toString()
            } > 0
        }.getOrThrow()

    override fun setUploadVerification(
        uploadId: Long,
        verification: Verification,
        error: String?
    ): Boolean =
        withTransaction {
            UploadFilesTable.update({ UploadFilesTable.id eq uploadId }) {
                it[UploadFilesTable.verified] = verification
                it[UploadFilesTable.errorMessage] = error
                it[UploadFilesTable.updatedAt] = Instant.now().toString()
            } > 0
        }.getOrThrow()

    override fun getDeletableFiles(fileIds: List<Long>): List<DeletableFile> = withTransaction {
        UploadFilesTable
            .select(
                UploadFilesTable.id,
                UploadFilesTable.importedFileId,
                UploadFilesTable.immichAssetId,
                UploadFilesTable.state,
                UploadFilesTable.verified
            )
            .where { UploadFilesTable.importedFileId inList fileIds }
            .groupBy { it[UploadFilesTable.importedFileId].value }
            .mapNotNull { (fileId, rows) ->
                if (rows.any {
                        it[UploadFilesTable.state] != UploadState.Success ||
                                it[UploadFilesTable.verified] != Verification.Verified ||
                                it[UploadFilesTable.immichAssetId] == null
                    }) return@mapNotNull null

                val row = rows.first()
                DeletableFile(
                    fileId = fileId,
                    uploadId = row[UploadFilesTable.id].value,
                    assetId = UUID.fromString(row[UploadFilesTable.immichAssetId])
                )
            }
    }.getOrThrow()

    override fun resetFailedUploadsOnUserId(userId: UUID): List<UUID> = withTransaction {
        val rows = UploadFilesTable
            .select(UploadFilesTable.uploadJobId)
            .where {
                (UploadFilesTable.immichUserId eq userId.toString()) and
                        (UploadFilesTable.state eq UploadState.Failure)
            }

        val jobIds = rows
            .mapNotNull { it[UploadFilesTable.uploadJobId] }
            .map(UUID::fromString)
            .distinct()

        UploadFilesTable.update({
            (UploadFilesTable.immichUserId eq userId.toString()) and
                    (UploadFilesTable.state eq UploadState.Failure)
        }) {
            it[UploadFilesTable.state] = UploadState.Pending
            it[UploadFilesTable.errorMessage] = null
            it[UploadFilesTable.updatedAt] = Instant.now().toString()
        }

        jobIds
    }.getOrThrow()

    override fun resetFailedUploadsOnJobId(jobId: UUID): Boolean =
        withTransaction {
            UploadFilesTable.update({
                        (UploadFilesTable.uploadJobId eq jobId.toString()) and
                        (UploadFilesTable.state eq UploadState.Failure)
            }) {
                it[UploadFilesTable.state] = UploadState.Pending
                it[UploadFilesTable.errorMessage] = null
                it[UploadFilesTable.updatedAt] = Instant.now().toString()
            } > 0
        }.getOrThrow()

    override fun getUploadsOnUserId(userId: UUID): List<PersistedUploadFile> =
        withTransaction {
            UploadFilesTable.getWhere {
                UploadFilesTable.immichUserId eq userId.toString()
            }
        }.getOrDefault(emptyList())

    override fun getUploadJobIdsOnUserId(userId: UUID): List<UUID> =
        withTransaction {
            UploadFilesTable
                .select(UploadFilesTable.uploadJobId)
                .where { UploadFilesTable.immichUserId eq userId.toString() }
                .mapNotNull { it[UploadFilesTable.uploadJobId]?.let(UUID::fromString) }
                .distinct()
        }.getOrDefault(emptyList())

    override fun getUploadSummary(userId: UUID): UploadSummary =
        withTransaction {
            val countsByState = UploadFilesTable
                .select(UploadFilesTable.state, UploadFilesTable.id.count())
                .where { UploadFilesTable.immichUserId eq userId.toString() }
                .groupBy(UploadFilesTable.state)
                .associate { row ->
                    row[UploadFilesTable.state] to row[UploadFilesTable.id.count()]
                }

            val lastUpdated = UploadFilesTable
                .select(UploadFilesTable.updatedAt)
                .where { UploadFilesTable.immichUserId eq userId.toString() }
                .orderBy(UploadFilesTable.updatedAt, SortOrder.DESC)
                .limit(1)
                .singleOrNull()
                ?.get(UploadFilesTable.updatedAt)
                ?.let {
                    try {
                        Instant.parse(it)
                    } catch (e: Exception) {
                        null
                    }
                }

            val ready = countsByState[UploadState.Pending] ?: 0L
            val inQueue = countsByState[UploadState.Uploading] ?: 0L
            val uploaded = countsByState[UploadState.Success] ?: 0L
            val failed = countsByState[UploadState.Failure] ?: 0L

            UploadSummary(
                userId = userId,
                totalUploads = ready + inQueue + uploaded + failed,
                totalReadyUploads = ready,
                totalInQueueUploads = inQueue,
                totalSucceededUploads = uploaded,
                totalFailedUploads = failed,
                lastUpdatedAt = lastUpdated
            )
        }.getOrDefault(
            UploadSummary(
                userId = userId,
                totalUploads = 0,
                totalReadyUploads = 0,
                totalInQueueUploads = 0,
                totalSucceededUploads = 0,
                totalFailedUploads = 0,
                lastUpdatedAt = null
            )
        )

    override fun getJobUploadSummaries(userId: UUID): List<UploadJobSummary> =
        withTransaction {
            UploadFilesTable
                .select(
                    UploadFilesTable.uploadJobId,
                    UploadFilesTable.state,
                    UploadFilesTable.id.count()
                )
                .where { UploadFilesTable.immichUserId eq userId.toString() }
                .groupBy(UploadFilesTable.uploadJobId, UploadFilesTable.state)
                .map { row ->
                    Triple(
                        row[UploadFilesTable.uploadJobId],
                        row[UploadFilesTable.state],
                        row[UploadFilesTable.id.count()].toInt()
                    )
                }
                .groupBy { it.first }
                .mapNotNull { (jobIdStr, rows) ->
                    if (jobIdStr == null) return@mapNotNull null

                    val counts = rows.associate { it.second to it.third }

                    UploadJobSummary(
                        userId = userId,
                        jobId = UUID.fromString(jobIdStr),
                        totalSuccess = counts[UploadState.Success] ?: 0,
                        totalFailure = counts[UploadState.Failure] ?: 0,
                        total = counts.values.sum(),
                        isRunning = false
                    )
                }
        }.getOrDefault(emptyList())
}

interface IUploadStore {

    /**
     * @param jobId Import job id, or generated for manual upload
     * @return fileIds from $reference ImportedFilesTable ID
     */
    fun getFileIdsAvailableForUpload(jobId: UUID): List<Long>
    fun getPendingUploadsOnJobId(jobId: UUID): List<PersistedUploadFile>
    fun getPendingUploadsOnUserId(userId: UUID): List<PersistedUploadFile>
    fun getSuccessfulUploadsOnUserId(userId: UUID): List<PersistedUploadFile>
    fun getSuccessfulUploadsOnJobId(jobId: UUID): List<PersistedUploadFile>
    fun getUnverifiedUploadsOnUserId(userId: UUID): List<PersistedUploadFile>
    fun getUnverifiedUploadsOnJobId(jobId: UUID): List<PersistedUploadFile>
    fun insertUploads(userId: UUID, jobId: UUID, fileIds: List<Long>): List<UploadJobItem>
    fun getUploadJobItems(jobId: UUID): List<UploadJobItem>
    fun getUploadJobItems(fileIds: List<Long>): List<UploadJobItem>
    fun getFileIdsInUploadsByJobId(jobId: UUID): List<Long>
    fun getAssetIdOnUploadId(uploadId: Long): UUID?
    fun getAssetIdOnUploadIds(uploadIds: List<Long>): Map<Long, UUID>
    fun getDeletableFiles(fileIds: List<Long>): List<DeletableFile>
    fun getChecksumOnFileId(fileId: Long): String?
    fun setUploadState(uploadId: Long, immichAssetId: UUID?, state: UploadState, error: String? = null): Boolean
    fun setUploadVerification(uploadId: Long, verification: Verification, error: String? = null): Boolean

    fun resetFailedUploadsOnJobId(jobId: UUID): Boolean
    fun resetFailedUploadsOnUserId(userId: UUID): List<UUID>
    fun getUploadsOnUserId(userId: UUID): List<PersistedUploadFile>
    fun getUploadJobIdsOnUserId(userId: UUID): List<UUID>
    fun getUploadSummary(userId: UUID): UploadSummary
    fun getJobUploadSummaries(userId: UUID): List<UploadJobSummary>
}