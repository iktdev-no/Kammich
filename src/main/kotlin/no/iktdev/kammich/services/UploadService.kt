package no.iktdev.kammich.services

import no.iktdev.kammich.database.tables.UploadFilesTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.models.shared.UploadState
import no.iktdev.kammich.models.shared.upload.UploadJobSummary
import no.iktdev.kammich.models.shared.upload.UploadSummary
import no.iktdev.kammich.repository.FileRepository
import no.iktdev.kammich.services.upload.UploaderService
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class UploadService(
    private val upladingService: UploaderService,
) {
    private val log = LoggerFactory.getLogger(FileRepository::class.java)

    fun getCheckForRemainingFiles(userId: UUID) {
        val result = withTransaction {
            UploadFilesTable.getWhere {
                (UploadFilesTable.immichUserId eq userId.toString())
            }
        }.getOrDefault(emptyList())
        log.info("Found ${result.size} in uploadTable")
    }

    fun resetFailedUploadsByUser(userId: UUID): Map<UUID, Boolean> {
        val resettableJobs = withTransaction {
            UploadFilesTable.select(UploadFilesTable.uploadJobId)
                .where { UploadFilesTable.immichUserId eq userId.toString() }
                .distinctBy { it[UploadFilesTable.uploadJobId] }
                .map { it[UploadFilesTable.uploadJobId].let { uploadJobId -> UUID.fromString(uploadJobId) } }
        }.getOrDefault(emptyList())
        return resettableJobs.associateWith { jobId ->
            resetFailedUploadJob(userId, jobId)
        }
    }

    fun resetFailedUploadJob(userId: UUID, jobId: UUID): Boolean {
        val resat = withTransaction {
            val ids = UploadFilesTable.select(UploadFilesTable.id)
                .where { (UploadFilesTable.immichUserId eq userId.toString()) and
                        (UploadFilesTable.uploadJobId eq jobId.toString()) and
                        (UploadFilesTable.state eq UploadState.Failure) }
            .map { it[UploadFilesTable.id].value }

            UploadFilesTable.update({ UploadFilesTable.id inList ids }) {
                it[updatedAt] = Instant.now().toString()
                it[state] = UploadState.Pending
            }
            ids
        }
        val result = resat.getOrDefault(emptyList())
        return if (resat.isSuccess && result.isNotEmpty()) {
            log.info("Job items for $jobId has been resetted")
            upladingService.uploadAlreadyExistingUploadEntries(userId, jobId, result)
            true
        } else {
            log.info("No job items found for $jobId that were eligible to reset")
            false
        }
    }

    fun getUploadSummary(userId: UUID): UploadSummary {
        val userIdStr = userId.toString()
        return withTransaction {
            // Hent tellere per state, og finn samtidig maksimale (nyeste) updatedAt for brukeren
            val countsByState = UploadFilesTable
                .select(UploadFilesTable.state, UploadFilesTable.id.count())
                .where { UploadFilesTable.immichUserId eq userIdStr }
                .groupBy(UploadFilesTable.state)
                .associate { row ->
                    val state = row[UploadFilesTable.state]
                    val count = row[UploadFilesTable.id.count()]
                    state to count
                }

            val lastUpdated = UploadFilesTable
                .select(UploadFilesTable.updatedAt)
                .where { UploadFilesTable.immichUserId eq userIdStr }
                .orderBy(UploadFilesTable.updatedAt, SortOrder.DESC)
                .limit(1)
                .singleOrNull()
                ?.get(UploadFilesTable.updatedAt) // Henter ut strengen direkte
                ?.let { timestampStr ->
                    try {
                        Instant.parse(timestampStr)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }


            val ready = countsByState[UploadState.Pending] ?: 0L
            val inQueue = countsByState[UploadState.Uploading] ?: 0L
            val uploaded = countsByState[UploadState.Success] ?: 0L
            val failed = countsByState[UploadState.Failure] ?: 0L
            val total = ready + inQueue + uploaded + failed

            UploadSummary(
                userId = userId,
                totalUploads = total,
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
    }


    fun getJobUploadSummaries(userId: UUID): List<UploadJobSummary> {
        val userIdStr = userId.toString()
        return withTransaction {
            // Hent tellere gruppert på både uploadJobId og state
            val rows = UploadFilesTable
                .select(UploadFilesTable.uploadJobId, UploadFilesTable.state, UploadFilesTable.id.count())
                .where { UploadFilesTable.immichUserId eq userIdStr }
                .groupBy(UploadFilesTable.uploadJobId, UploadFilesTable.state)
                .map { row ->
                    val jobIdStr = row[UploadFilesTable.uploadJobId]
                    val state = row[UploadFilesTable.state]
                    val count = row[UploadFilesTable.id.count()].toInt() // Konverter evt. fra Long til Int avhengig av din Exposed-versjon
                    Triple(jobIdStr, state, count)
                }

            // Grupper resultatene per job-ID i Kotlin
            rows.groupBy { it.first }
                .mapNotNull { (jobIdStr, jobRows) ->
                    if (jobIdStr == null) return@mapNotNull null

                    val countsByState = jobRows.associate { it.second to it.third }

                    val success = countsByState[UploadState.Success] ?: 0
                    val failure = countsByState[UploadState.Failure] ?: 0
                    val total = countsByState.values.sum()

                    UploadJobSummary(
                        userId = userId,
                        jobId = UUID.fromString(jobIdStr),
                        totalSuccess = success,
                        totalFailure = failure,
                        total = total
                    )
                }
        }.getOrDefault(emptyList())
    }


    fun uploadFile(userId: UUID, fileId: Long) {
        upladingService.uploadSingleFile(fileId, userId)

    }



}