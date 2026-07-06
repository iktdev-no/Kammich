package no.iktdev.kammich.storage.media

import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.ImportedFilesTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.models.FileType
import no.iktdev.kammich.models.shared.RemoteFile
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileNotFoundException

@Service
class PhotoService(
    private val config: ConfigService,
): MediaService {
    private val log = LoggerFactory.getLogger(PhotoService::class.java)
    // Cacher index per deviceId


    fun getAllPhotos() {
        log.info("Getting all photos")
        val contentRoot = config.getConfig().mediaPath.let { File(it) }

    }

    override fun getFile(deviceId: Int, filename: String): FileSystemResource {
        // 1. Hent serialnummer fra DB
        val serial =  withTransaction {
            DevicesTable.select(DevicesTable.serialNumber)
                .where { (DevicesTable.id eq deviceId) }
                .singleOrNull()?.get(DevicesTable.serialNumber)
        }.getOrNull() ?: throw IllegalArgumentException("Enhet med ID $deviceId finnes ikke")

        // 2. Konstruer stien: basePath / serial / filename
        val mediaPath = config.getConfig().mediaPath
        val file = File(mediaPath, "$serial/$filename")

        // 3. Sikkerhetssjekk: Eksisterer filen?
        if (!file.exists() || !file.isFile) {
            throw FileNotFoundException("Filen $filename ble ikke funnet på enhet $serial")
        }

        // 4. Returner som Resource for Spring
        return FileSystemResource(file)
    }

    override fun getPagedFiles(deviceId: Int, page: Int, size: Int): List<RemoteFile> {
        return withTransaction {
            ImportedFilesTable
                .selectAll()
                .where { ImportedFilesTable.deviceId eq deviceId }
                .limit(size)
                .offset((page * size).toLong()) // Skill ut offset her
                .where { ImportedFilesTable.fileType eq FileType.IMAGE }
                .map { it ->
                    RemoteFile(
                        id = it[ImportedFilesTable.id].value,
                        deviceId = it[ImportedFilesTable.deviceId].value,
                        fileName = it[ImportedFilesTable.fileName],
                    )
                }
        }.getOrNull() ?: emptyList()
    }

    override fun getPagedFiles(page: Int, size: Int): List<RemoteFile> {
        return withTransaction {
            ImportedFilesTable
                .selectAll()
                .limit(size)
                .offset((page * size).toLong()) // Skill ut offset her
                .where { ImportedFilesTable.fileType eq FileType.IMAGE }
                .map { it ->
                    RemoteFile(
                        id = it[ImportedFilesTable.id].value,
                        deviceId = it[ImportedFilesTable.deviceId].value,
                        fileName = it[ImportedFilesTable.fileName],
                    )
                }
        }.getOrNull() ?: emptyList()
    }

    override fun getTotalCount(): Long {
        return withTransaction {
            ImportedFilesTable.selectAll()
                .where { ImportedFilesTable.fileType eq FileType.IMAGE }.count()
        }.getOrDefault(0)
    }

}