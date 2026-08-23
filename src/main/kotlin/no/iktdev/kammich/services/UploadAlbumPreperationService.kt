package no.iktdev.kammich.services

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory
import no.iktdev.kammich.database.models.PersistedImportedFile
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.UploadFileAlbumsTable
import no.iktdev.kammich.database.tables.UploadFilesTable
import no.iktdev.kammich.database.tables.UploadFilesTable.updatedAt
import no.iktdev.kammich.database.tables.getDeviceSerialNumber
import no.iktdev.kammich.models.shared.Album
import no.iktdev.kammich.models.shared.UploadState
import no.iktdev.kammich.repository.FileRepository
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Service
import java.io.File
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class UploadAlbumPreperationService(
    private val albumService: AlbumService,
    private val fileRepository: FileRepository,
    private val configService: ConfigService
) {

    suspend fun processAlbumTagging() {
        val albums = albumService.getAlbums().filter { it.startDate != null }
        if (albums.isEmpty()) return

        // Finn filer som er ferdig lastet opp og har fått asset ID,
        // men som kanskje ikke har fått koblet sine album ennå
        val uploadedFiles = UploadFilesTable.getWhere {
            (UploadFilesTable.state eq UploadState.Success ) and (UploadFilesTable.immichAssetId.isNotNull())
        }

        for (upload in uploadedFiles) {
            val imported = fileRepository.getFileById(upload.importedFileId) ?: continue
            val deviceSerial = DevicesTable.getDeviceSerialNumber(imported.deviceId) ?: continue

            val file = imported.getFile(deviceSerial, configService)
            val exifDate = if (file.exists()) file.getExifTimestamp() else null
            val effectiveTimestamp = exifDate ?: try {
                ZonedDateTime.parse(imported.importedAt, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant()
            } catch (e: Exception) {
                Instant.parse(imported.importedAt.replace(Regex("\\[.*\\]"), ""))
            }

            val matchingAlbum = findAlbumForTimestamp(effectiveTimestamp, albums) ?: continue

            // Sjekk om koblingen finnes fra før i UPLOAD_FILE_ALBUMS
            val linkExists = UploadFileAlbumsTable.selectAll()
                .where { (UploadFileAlbumsTable.uploadFileId eq upload.id) and // Brukte upload.importedFileId her før!
                        (UploadFileAlbumsTable.albumId eq matchingAlbum.id) }
                .singleOrNull() != null

            if (!linkExists) {
                // Opprett koblingen, klar for at en worker pusher den til Immich API
                UploadFileAlbumsTable.insert {
                    it[uploadFileId] = upload.id
                    it[albumId] = matchingAlbum.id
                    it[updatedAt] = Instant.now().toString()
                }
            }
        }
    }

    private fun PersistedImportedFile.getFile(deviceSn: String, config: ConfigService): File {
        val mediaPath = config.getConfig().mediaPath
        return File(mediaPath, "$deviceSn/${this.fileName}")
    }

    private fun File.getExifTimestamp(): Instant? {
        return try {
            val metadata = ImageMetadataReader.readMetadata(this)
            val directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
            directory?.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)?.toInstant()
        } catch (e: Exception) {
            null
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