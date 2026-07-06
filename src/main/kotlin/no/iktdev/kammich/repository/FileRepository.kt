package no.iktdev.kammich.repository

import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.ImportedFilesTable
import no.iktdev.kammich.database.tables.getDeviceId
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.getFileType
import no.iktdev.kammich.models.shared.files.KFile
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.time.ZonedDateTime

@Component
class FileRepository {
    private val log = LoggerFactory.getLogger(FileRepository::class.java)

    fun getFilesToImport(deviceSN: String, files: List<KFile>): List<KFile> {
        val deviceId = DevicesTable.getDeviceId(deviceSN) ?: run {
            log.info("Device $deviceSN is not found, all files to import")
            return files
        }
        val ffiles = files.map { file -> file.name }
        val existing = withTransaction {
            ImportedFilesTable.select(ImportedFilesTable.fileName)
                .where {
                    (ImportedFilesTable.deviceId eq deviceId) and
                            (ImportedFilesTable.fileName inList ffiles)
                }
                .map { it[ImportedFilesTable.fileName] }
                .toSet()
        }.getOrDefault(emptyList())
        return files.filterNot { it.name in existing }
    }

    fun isFileImported(deviceId: Int, fileName: String, fileSize: Long): Boolean {
        return withTransaction {
            ImportedFilesTable
                .selectAll().where {
                    (ImportedFilesTable.deviceId eq deviceId) and
                            (ImportedFilesTable.fileName eq fileName) and
                            (ImportedFilesTable.fileSize eq fileSize)
                }
                .count() > 0
        }.getOrNull() ?: false
    }

    fun saveFiles(deviceId: Int, files: List<Pair<File, ZonedDateTime>>): Boolean {
        return withTransaction {
            ImportedFilesTable.batchInsert(files) {
                this[ImportedFilesTable.deviceId] = deviceId
                this[ImportedFilesTable.fileName] = it.first.name
                this[ImportedFilesTable.fileType] = it.first.getFileType()
                this[ImportedFilesTable.fileSize] = it.first.length()
                this[ImportedFilesTable.extension] = it.first.extension
                this[ImportedFilesTable.importedAt] = it.second.toString()
            }
        }.isSuccess
    }

    fun saveFile(deviceId: Int, file: File, importedAt: String) {
        withTransaction {
            ImportedFilesTable.insert {
                it[this.deviceId] = deviceId // Her bruker du ID fra DB
                it[this.fileName] = file.name
                it[this.fileSize] = file.length()
                it[this.importedAt] = importedAt
            }
        }
    }
}