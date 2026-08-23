package no.iktdev.kammich.services

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import no.iktdev.kammich.between
import no.iktdev.kammich.database.models.PersistedUploadFile
import no.iktdev.kammich.database.tables.AlbumsTable
import no.iktdev.kammich.database.tables.UploadFilesTable
import no.iktdev.kammich.database.tables.ImportedFilesTable
import no.iktdev.kammich.database.tables.UploadFileAlbumsTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.getExifTimestamp
import no.iktdev.kammich.immich.ImmichApi
import no.iktdev.kammich.immich.client.ImmichClientFactory
import no.iktdev.kammich.immich.context.ImmichUserContext
import no.iktdev.kammich.immich.services.ImmichContextService
import no.iktdev.kammich.models.internal.PersistedAlbum
import no.iktdev.kammich.models.internal.events.UploadCompletedEvent
import no.iktdev.kammich.models.shared.Album
import no.iktdev.kammich.models.shared.AlbumCreateRequest
import no.iktdev.kammich.models.shared.AlbumUpdateRequest
import no.iktdev.kammich.models.shared.RemoteFile
import no.iktdev.kammich.models.shared.UploadState
import no.iktdev.kammich.repository.FileRepository
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.leftJoin
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class AlbumService(
    private val contextService: ImmichContextService,
    private val immichUserContext: ImmichUserContext,
    private val immichClientFactory: ImmichClientFactory,
    private val fileRepository: FileRepository,
    private val configService: ConfigService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    fun getAlbums(): List<Album> {
        val immichUserId = immichUserContext.getCurrentUserId() ?: run {
            log.error("No user in context, returning empty list")
            return emptyList()
        }
        return withTransaction {
            val albums = AlbumsTable.getWhere {
                AlbumsTable.immichUserId eq immichUserId.toString()
            }

            albums.map { album ->

                // Telle filer knyttet til dette albumet via koblingstabellen
                val totalFiles = UploadFileAlbumsTable.selectAll()
                    .where { UploadFileAlbumsTable.albumId eq album.id }
                    .count()

                // Bruk leftJoin slik at albumet beholdes selv om det ikke har noen filer enda
                val sampleFileRow = UploadFileAlbumsTable
                    .leftJoin(UploadFilesTable, { uploadFileId }, { UploadFilesTable.id })
                    .leftJoin(ImportedFilesTable, { UploadFilesTable.importedFileId }, { ImportedFilesTable.id })
                    .selectAll()
                    .where { UploadFileAlbumsTable.albumId eq album.id }
                    .limit(1)
                    .singleOrNull()

                val sampleFile = sampleFileRow?.let { row ->
                    // Sjekk at ImportedFilesTable faktisk ble med i left join-en (at ID-en ikke er null)
                    row.getOrNull(ImportedFilesTable.id)?.let { importedFileId ->
                        RemoteFile(
                            id = importedFileId.value,
                            deviceId = row[ImportedFilesTable.deviceId].value,
                            fileName = row[ImportedFilesTable.fileName]
                        )
                    }
                }

                album.toAlbum(totalFiles, sampleFile)
            }
        }.getOrDefault(emptyList())
    }

    fun createAlbum(request: AlbumCreateRequest): Long {
        val userId = immichUserContext.getCurrentUser()?.id ?: run {
            log.error("No user in context, returning empty list")
            throw IllegalArgumentException("No user in context, returning empty long")
        }
        val session = contextService.findSessionsByUserId(userId) ?: throw RuntimeException("No session found for $userId")
        val client = immichClientFactory.create(session.serverUrl)
        val albumId = client.createAlbum(session.apiKey, request.albumName, request.description)

        val created = withTransaction {
            AlbumsTable.insertAndGetId {
                it[immichAlbumId] = albumId.toString()
                it[immichUserId] = userId.toString()
                it[title] = request.albumName
                it[description] = request.description
                it[startDate] = request.startDate
                it[endDate] = request.endDate
                it[use] = false
                it[createdAt] = Instant.now().toString()
            }.value
        }.getOrThrow()
        serviceScope.launch { synchTimeSlot(created) }
        return created
    }


    fun updateAlbum(id: Long, request: AlbumUpdateRequest): Boolean {
        val userId = immichUserContext.getCurrentUserId() ?: run {
            log.error("No user in context")
            throw IllegalArgumentException("No user in context")
        }
        log.info("Updating album $id, ${Gson().toJson(request)}")

        // 1. Hent eksisterende album fra DB for å validere at det finnes og hente immichAlbumId
        val existingAlbum = withTransaction {
            AlbumsTable.getWhere {
                (AlbumsTable.id eq id) and (AlbumsTable.immichUserId eq userId.toString())
            }.singleOrNull()
        }.getOrNull() ?: return false

        // 2. Oppdater Immich BARE hvis navn eller beskrivelse faktisk er med i requesten
        val shouldUpdateImmich = request.albumName != null || request.description != null
        log.debug("Should update immich? $shouldUpdateImmich with id ${existingAlbum.immichAlbumId}")
        if (shouldUpdateImmich && existingAlbum.immichAlbumId != null) {
            val session = contextService.findSessionsByUserId(userId) ?: throw RuntimeException("No session found for $userId")
            val client = immichClientFactory.create(session.serverUrl)

            val newTitle = request.albumName ?: existingAlbum.name
            val newDescription = request.description ?: existingAlbum.description

            try {
                client.updateAlbum(session.apiKey, existingAlbum.immichAlbumId, newTitle, newDescription)
            } catch (e: Exception) {
                log.error("Feil ved oppdatering av album i Immich for ID $id: ${e.message}")
                return false // Avbryt hvis Immich-oppdatering feiler, så databasen forblir i sync
            }
        }

        // 3. Utfør selve databasedelen (lagrer alt, inkludert start/end-date og use)
        val updatedRows = withTransaction {
            AlbumsTable.update({
                (AlbumsTable.id eq id) and (AlbumsTable.immichUserId eq userId.toString())
            }) { row ->
                request.albumName?.let { row[AlbumsTable.title] = it }
                request.description?.let { row[AlbumsTable.description] = it }
                request.startDate?.let { row[AlbumsTable.startDate] = it }
                request.endDate?.let { row[AlbumsTable.endDate] = it }
                request.use?.let { row[AlbumsTable.use] = it }
            }
        }.getOrDefault(0)

        if (updatedRows <= 0) {
            return false
        }

        // 4. Start bakgrunnsjobb for tidsrom-synk uavhengig av om Immich ble oppdatert
        // (siden start/end-date kan ha endret seg, noe som påvirker lokale filer)
        serviceScope.launch { synchTimeSlot(id) }

        return true
    }

    fun deleteAlbum(id: Long): Boolean {
        val userId = immichUserContext.getCurrentUserId() ?: run {
            log.error("No user in context, returning empty list")
            throw IllegalArgumentException("No user in context, returning")
        }
        return withTransaction {
            AlbumsTable.deleteWhere { (AlbumsTable.id eq id) and (AlbumsTable.immichUserId eq userId.toString()) } > 0
        }.getOrDefault(false)
    }

    @EventListener(UploadCompletedEvent::class)
    fun onUploadCompleted(event: UploadCompletedEvent) {
        val albums = getAlbums()
        albums.forEach { album ->
            synchTimeSlot(album.id)
        }
    }

    fun synchTimeSlot(id: Long) {
        val album = withTransaction {
            AlbumsTable.getWhere { AlbumsTable.id eq id }
                .singleOrNull()
        }.getOrNull() ?: run {
            log.error("No album found for $id")
            return
        }
        if (!album.use) {
            log.info("Album is not enabled: ${album.name}")
        }
        addFilesWithinTimeTo(album)
        val newFiles = withTransaction {
            val uploadIds = UploadFileAlbumsTable.selectAll()
                .where { (UploadFileAlbumsTable.albumId eq album.id) and (UploadFileAlbumsTable.state eq UploadState.Pending) }
                .map { it[UploadFileAlbumsTable.uploadFileId].value }

            if (uploadIds.isEmpty()) {
                emptyList()
            } else {
                UploadFilesTable.getWhere { UploadFilesTable.id inList uploadIds }
            }
        }.getOrNull() ?: emptyList()

        if (newFiles.isEmpty()) {
            log.info("Ingen nye filer å hente for album ${album.id}")
        }
        val toUpload = newFiles.filter { it.immichAssetId != null }
        addAssetsToAlbum(album, toUpload)
    }

    fun addFilesWithinTimeTo(album: PersistedAlbum) {
        val start = album.startDate ?: run {
            log.error("No start date for $album")
            return
        }
        val end = album.endDate ?: run {
            log.error("No end date for $album")
            return
        }
        log.info("Using timespan: ${start.toString()} to ${end.toString()}")
        val uploads = withTransaction {
            UploadFilesTable.getWhere { UploadFilesTable.state eq UploadState.Success }
        }.getOrDefault(emptyList())
        if (uploads.isEmpty()) {
            log.info("Ingen vellykkede opplastinger funnet")
            return
        }
        val fileEntries = withTransaction {
            val fileIds = uploads.map { upload -> upload.importedFileId}
            ImportedFilesTable.getAllWithDevice { ImportedFilesTable.id inList fileIds }
        }.getOrThrow()
        val mediaPath = configService.getConfig().mediaPath
        val uploadToFileTime = uploads.mapNotNull { upload ->
            val fileEntry = fileEntries.find { it -> it.id == upload.importedFileId }
            if (fileEntry != null) {
                val file = fileEntry.getFile(mediaPath)
                val useCreatedTime = file.getExifTimestamp() ?: Instant.ofEpochMilli(file.lastModified())
                log.debug("${file.absolutePath} - ${file.length()} - $useCreatedTime")
                upload.id to useCreatedTime
            } else {
                null
            }
        }.toMap()
        val toAdd = uploadToFileTime.filter { it.value.between(start, end) }
        if (toAdd.isNotEmpty()) {
            withTransaction {
                toAdd.forEach { (uploadFileID, instant) ->
                    UploadFileAlbumsTable.insertIgnore {
                        it[uploadFileId] = uploadFileID
                        it[albumId] = album.id
                        it[state] = UploadState.Pending
                    }
                }
            }
        }
    }

    private fun addAssetsToAlbum(album: PersistedAlbum, assets: List<PersistedUploadFile>): Boolean {
        if (assets.isEmpty()) return true
        if (album.immichAlbumId == null) return false
        val userId = immichUserContext.getCurrentUser()?.id ?: run {
            log.error("No user in context, returning empty list")
            throw IllegalArgumentException("No user in context, returning empty long")
        }
        val session = contextService.findSessionsByUserId(userId) ?: throw RuntimeException("No session found for $userId")
        val client = immichClientFactory.create(session.serverUrl)

        // 1. Prøv bulk-operasjon først
        val assetIds = assets.mapNotNull { it.immichAssetId}
        try {
            val success = client.addPhotoToAlbum(session.apiKey, album.immichAlbumId, assetIds)
            if (success) {
                updateUploadStateForAssets(album.id, assets.map { it.id }, UploadState.Success)
                return true
            }
        } catch (e: Exception) {
            log.warn("Bulk addPhotoToAlbum feilet, faller tilbake til enkeltvis prosessering: ${e.message}")
        }

        val uploadStatuses = assetIds.associateWith { immichFileId ->
            try {
                client.addPhotoToAlbum(session.apiKey, album.immichAlbumId, listOf(immichFileId))
            } catch (e: Exception) {
                val itemIds = assets.find { it.immichAssetId == immichFileId }.let { it?.id } ?: return@associateWith false
                updateUploadStateForAssets(album.id, listOf(itemIds), UploadState.Failure, e.message)
                false
            }
        }
        if (uploadStatuses.none { it.value }) {
            updateUploadStateForAssets(album.id, assets.map { it.id }, UploadState.Failure)
            return false
        }

        val success = uploadStatuses.filter { it.value }.keys
        val uploadFileIdSuccess = assets.filter { it.immichAssetId in success }.map { it.id }
        updateUploadStateForAssets(album.id, uploadFileIdSuccess, UploadState.Success)

        return success.isNotEmpty()
    }

    private fun updateUploadStateForAssets(albumId: Long, uploadedFileId: List<Long>, useState: UploadState, errorMessage: String? = null) {
        if (uploadedFileId.isEmpty()) return
        withTransaction {
            UploadFileAlbumsTable.update({
                (UploadFileAlbumsTable.albumId eq albumId) and (UploadFileAlbumsTable.uploadFileId inList uploadedFileId)
            }) {
                it[state] = useState
                it[UploadFileAlbumsTable.errorMessage] = errorMessage
            }
        }
    }

}