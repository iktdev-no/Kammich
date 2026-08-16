package no.iktdev.kammich.services

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory
import kotlinx.coroutines.*
import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.database.models.PersistedImportedFile
import no.iktdev.kammich.database.tables.DeviceOwnerTable
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.ImportJobOwnerTable
import no.iktdev.kammich.database.tables.UploadFilesTable
import no.iktdev.kammich.database.tables.UploadState
import no.iktdev.kammich.database.tables.getDeviceSerialNumber
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.immich.services.ImmichService
import no.iktdev.kammich.immich.context.ImmichUserContext
import no.iktdev.kammich.models.internal.events.ImportJobCompletedEvent
import no.iktdev.kammich.models.shared.Album
import no.iktdev.kammich.repository.FileRepository
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.io.File
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class UploadPreparationService(
    private val immichUserContext: ImmichUserContext,
    private val immichService: ImmichService,
    private val albumService: AlbumService,
    private val configService: ConfigService,
    private val fileRepository: FileRepository
) {
    private val log = LoggerFactory.getLogger(UploadPreparationService::class.java)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @EventListener
    fun onImportCompleted(event: ImportJobCompletedEvent) {
        serviceScope.launch {
            try {
                log.info("Starter automatisk klargjøring for import-jobb: ${event.jobId}")

                val userId = immichUserContext.getCurrentUserId()?.toString() ?: run {
                    log.warn("Ingen aktiv bruker i kontekst for import-jobb ${event.jobId}")
                    return@launch
                }

                if (isSingleAndOnlyUser(userId) && configService.getConfig().autoClaimImportsWhenSingleUser) {
                    claimAndPrepareImportJob(event.jobId.toString(), userId, event.deviceId)
                } else {
                    if (userOwnsDevice(event.deviceId, userId)) {
                        claimAndPrepareImportJob(event.jobId.toString(), userId, event.deviceId)
                    } else {
                        log.warn("Bruker $userId eier ikke enheten (${event.deviceId}), og systemet har flere brukere. Kan ikke auto-claime.")
                        return@launch
                    }
                }

            } catch (e: Exception) {
                log.error("Feil under automatisk klargjøring av import-jobb ${event.jobId}", e)
            }
        }
    }

    fun processUnclaimedJobsForDevice(deviceSn: String, userId: String) {
        // 2. Start bakgrunnsjobb for å finne og claime/klargjøre uclaimede import-jobber for denne enheten
        serviceScope.launch {
            try {
                val unclaimedJobIds = findUnclaimedJobIdsForDevice(deviceSn)
                log.info("Fant ${unclaimedJobIds.size} uclaimede import-jobber for enhet $deviceSn som skal knyttes til bruker $userId")

                unclaimedJobIds.forEach { jobId ->
                    // Gjenbruker logikken du allerede har!
                    claimAndPrepareImportJob(jobId, userId, deviceSn)
                }
            } catch (e: Exception) {
                log.error("Feil under opprydding og klargjøring av uclaimede jobber for enhet $deviceSn", e)
            }
        }
    }

    private fun findUnclaimedJobIdsForDevice(deviceSn: String): List<String> {
        return withTransaction {
            // 1. Alle jobber som tilhører denne enheten
            val allJobIdsForDevice = fileRepository.getFilesByDeviceSn(deviceSn)
                .map { it.importJob.toString() }
                .distinct()

            if (allJobIdsForDevice.isEmpty()) return@withTransaction emptyList()

            // 2. Finn hvilke av DISSE som allerede finnes i eiertabellen
            val alreadyClaimedJobIds = ImportJobOwnerTable.selectAll()
                .where { ImportJobOwnerTable.importJob inList allJobIdsForDevice }
                .map { it[ImportJobOwnerTable.importJob] }

            // 3. Diffen er de som ikke er claimet
            allJobIdsForDevice.filter { it !in alreadyClaimedJobIds }
        }.getOrDefault(emptyList())
    }

    fun processUnclaimedJobsForUser(importJobId: UUID, userId: String) {
        serviceScope.launch {
            claimAndPrepareImportJob(importJobId.toString(), userId, null)
        }
    }



    fun isSingleAndOnlyUser(userId: String): Boolean {
        val allUsers = immichService.getUsersWithAccesses().map { it.user }
        return allUsers.size == 1 && allUsers.first().id.toString() == userId
    }

    private fun userOwnsDevice(deviceSn: String, userId: String): Boolean {
        return withTransaction {
            DeviceOwnerTable.selectAll()
                .where { (DeviceOwnerTable.deviceSN eq deviceSn) and (DeviceOwnerTable.immichUserId eq userId) }
                .singleOrNull() != null
        }.getOrDefault(false)
    }


    suspend fun claimAndPrepareImportJob(importJobId: String, userId: String, deviceSn: String?): Boolean {
        val claimed = withTransaction {
            ImportJobOwnerTable.upsert {
                it[ImportJobOwnerTable.importJob] = importJobId
                it[ImportJobOwnerTable.immichUserId] = userId
            }
        }.isSuccess

        if (!claimed) {
            log.error("Klarte ikke å lagre import-jobb eierskap for $importJobId")
            return false
        }

        prepareUploads(importJobId, userId, deviceSn)
        log.info("Ferdig med klargjøring for import-jobb: $importJobId")
        return true
    }

    private fun PersistedImportedFile.getFile(deviceSn: String): File {
        val mediaPath = configService.getConfig().mediaPath
        return File(mediaPath, "$deviceSn/${this.fileName}")
    }

    fun File.getExifTimestamp(): Instant? {
        return try {
            val metadata = ImageMetadataReader.readMetadata(this)
            val directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
            directory?.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)?.toInstant()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun prepareUploads(jobId: String, userId: String, deviceSn: String? = null) {
        val albums = albumService.getAlbums().filter { it.startDate != null }
        val importedFiles = fileRepository.getFilesByJobId(jobId)

        if (importedFiles.isEmpty()) return

        val deviceSerialNumber = deviceSn ?: DevicesTable.getDeviceSerialNumber(importedFiles.first().id) ?: run {
            log.error("Couldn't get device serial number for $jobId")
            return
        }

        withTransaction {
            importedFiles.forEach { imported ->
                val file = imported.getFile(deviceSerialNumber)

                val exifDate = if (file.exists()) file.getExifTimestamp() else null
                val effectiveTimestamp = exifDate ?:
                    ZonedDateTime.parse(imported.importedAt, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant()

                val matchingAlbum = findAlbumForTimestamp(effectiveTimestamp, albums)

                UploadFilesTable.insert {
                    it[importedFileId] = imported.id
                    it[immichUserId] = userId
                    it[albumId] = matchingAlbum?.id
                    it[state] = UploadState.READY
                    it[retryCount] = 0
                    it[updatedAt] = Instant.now().toString()
                }
            }
        }
    }

    private fun findAlbumForTimestamp(timestamp: Instant?, albums: List<Album>): Album? {
        if (timestamp == null) return null

        return albums.find { album ->
            val start = album.startDate
            val end = album.endDate
            when {
                start != null && end != null -> timestamp in start..end
                start != null -> timestamp >= start
                else -> false
            }
        }
    }
}