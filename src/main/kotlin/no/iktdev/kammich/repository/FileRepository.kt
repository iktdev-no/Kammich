package no.iktdev.kammich.repository

import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.ImportedFilesTable
import no.iktdev.kammich.database.tables.ImportedFilesTable.toPersisted
import no.iktdev.kammich.database.tables.getDeviceId
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.ensureWritable
import no.iktdev.kammich.getFileType
import no.iktdev.kammich.models.FileHash
import no.iktdev.kammich.models.internal.KFile
import no.iktdev.kammich.models.shared.DeviceImport
import no.iktdev.kammich.models.shared.ImportFile
import no.iktdev.kammich.models.shared.FileImportState
import no.iktdev.kammich.models.shared.device.RemovableDevice
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.io.File
import java.time.Instant
import java.time.ZonedDateTime

@Component
class FileRepository(
    private val configService: ConfigService,
    private val eventPublisher: ApplicationEventPublisher,

) {
    private val log = LoggerFactory.getLogger(FileRepository::class.java)

    fun getStorageLocationForImport(device: RemovableDevice): File? {
        val mediaRoot = File(configService.getConfig().mediaPath).ensureWritable(
            eventPublisher, "FileRepository-mediaRoot"
        ) ?: return null

        return File(mediaRoot, device.id).ensureWritable(
            eventPublisher, "FileRepository-deviceFolder-${device.id}"
        ) ?: run {
            log.error("Device ${device.id} has no storage location")
            null
        }
    }

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

    fun saveFile(deviceId: Int, file: File, importedAt: ZonedDateTime, hash: FileHash) {
        withTransaction {
            ImportedFilesTable.insert {
                it[this.deviceId] = deviceId // Her bruker du ID fra DB
                it[this.fileName] = file.name
                it[this.fileType] = file.getFileType()
                it[this.fileSize] = file.length()
                it[this.extension] = file.extension
                it[this.checksum] = hash.hash
                it[this.checksumType] = hash.method.name
                it[this.importedAt] = importedAt.toString()
            }
        }
    }



    fun getImportHistory(): List<DeviceImport> {
        // 1. Hent alle enheter slik at vi har navn, serialNumber osv.
        val devices = DevicesTable.getDevices().associateBy { it.id }

        return withTransaction {
            // 2. Hent alle importerte filer og gruppér dem på deviceId
            val filesByDevice = ImportedFilesTable.selectAll()
                .map { it.toPersisted() }
                .groupBy { it.deviceId }

            // 3. Bygg opp DeviceImport for hver enhet som har filer i historikken
            filesByDevice.mapNotNull { (dbDeviceId, persistedFiles) ->
                val device = devices[dbDeviceId] ?: return@mapNotNull null

                val sharedFiles = persistedFiles.map { file ->
                    ImportFile(
                        file = file.fileName,
                        isNew = false, // Allerede importert (historikk)
                        state = FileImportState.Success
                    )
                }

                // Finn tidspunktet for den første importen (eller satt til nå hvis feiler)
                val firstImportedAt = persistedFiles.minOfOrNull {
                    runCatching { Instant.parse(it.importedAt) }.getOrNull() ?: Instant.now()
                } ?: Instant.now()

                DeviceImport(
                    deviceId = device.serialNumber, // Bruker serialNumber som ID ut mot FE (slik du gjorde tidligere)
                    deviceName = device.name.ifBlank { device.model ?: device.serialNumber },
                    started = firstImportedAt,
                    totalFiles = sharedFiles.size,
                    completedFiles = sharedFiles.size,
                    currentFileName = null,
                    files = sharedFiles
                )
            }
        }.getOrDefault(emptyList())
    }
}