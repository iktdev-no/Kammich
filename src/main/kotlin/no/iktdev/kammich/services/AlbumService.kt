package no.iktdev.kammich.services

import no.iktdev.kammich.database.tables.AlbumsTable
import no.iktdev.kammich.database.tables.UploadFilesTable
import no.iktdev.kammich.database.tables.ImportedFilesTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.immich.ImmichUserContext
import no.iktdev.kammich.models.shared.Album
import no.iktdev.kammich.models.shared.AlbumCreateRequest
import no.iktdev.kammich.models.shared.AlbumUpdateRequest
import no.iktdev.kammich.models.shared.RemoteFile
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AlbumService(
    private val immichUserContext: ImmichUserContext
) {
    private val log = LoggerFactory.getLogger(javaClass)

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

                val totalFiles = UploadFilesTable.selectAll()
                    .where { UploadFilesTable.albumId eq album.id }
                    .count()

                // Hent første fil via join mellom UploadFilesTable og ImportedFilesTable
                val sampleFileRow = UploadFilesTable.innerJoin(ImportedFilesTable)
                    .selectAll()
                    .where { UploadFilesTable.albumId eq album.id }
                    .limit(1)
                    .singleOrNull()

                val sampleFile = sampleFileRow?.let {
                    RemoteFile(
                        id = it[ImportedFilesTable.id].value,
                        deviceId = it[ImportedFilesTable.deviceId].value, // Hent ut .value for ID
                        fileName = it[ImportedFilesTable.fileName]
                    )
                }
                album.toAlbum(totalFiles, sampleFile)
            }
        }.getOrDefault(emptyList())
    }

    fun createAlbum(request: AlbumCreateRequest): Long {
        val userId = immichUserContext.getCurrentUserId() ?: run {
            log.error("No user in context, returning empty list")
            throw IllegalArgumentException("No user in context, returning empty long")
        }
        return withTransaction {
            AlbumsTable.insert {
                it[immichUserId] = userId.toString()
                it[title] = request.albumName
                it[description] = request.description
                it[startDate] = request.startDate
                it[endDate] = request.endDate
                it[use] = false
                it[createdAt] = Instant.now().toString()
            }[AlbumsTable.id].value
        }.getOrElse { throw IllegalStateException("Klarte ikke å opprette album") }
    }

    fun updateAlbum(id: Long, request: AlbumUpdateRequest): Boolean {
        val userId = immichUserContext.getCurrentUserId() ?: run {
            log.error("No user in context, returning empty list")
            throw IllegalArgumentException("No user in context, returning empty long")
        }

        return withTransaction {
            if (request.use == true) {
                AlbumsTable.update { it[use] = false }
            }
            AlbumsTable.update({ (AlbumsTable.id eq id) and (AlbumsTable.immichUserId eq userId.toString()) }) { row ->
                request.albumName?.let { row[AlbumsTable.title] = it }
                request.description?.let { row[AlbumsTable.description] = it }
                request.startDate?.let { row[AlbumsTable.startDate] = it }
                request.endDate?.let { row[AlbumsTable.endDate] = it }
                request.use?.let { row[AlbumsTable.use] = it }
            } > 0
        }.getOrDefault(false)
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
}