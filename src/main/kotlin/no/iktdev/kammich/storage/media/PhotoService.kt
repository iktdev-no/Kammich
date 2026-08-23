package no.iktdev.kammich.storage.media

import no.iktdev.kammich.services.ConfigService
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.DevicesTable.toPersistedDevice
import no.iktdev.kammich.database.tables.ImportedFilesTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.models.FileType
import no.iktdev.kammich.models.shared.RemoteFile
import no.iktdev.kammich.models.shared.device.PhotoDevice
import no.iktdev.kammich.storage.Thumbnail
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

    fun getPhotoDevices(): List<PhotoDevice> {
        return withTransaction {
            DevicesTable.selectAll()
                .mapNotNull { it.toPersistedDevice() }
        }.getOrDefault(emptyList())
            .map { device ->
            PhotoDevice(
                name = device.name,
                serialNumber = device.serialNumber,
                manufacturer = device.manufacturer,
                model = device.model,
            )
        }
    }


    fun getAllPhotos() {
        log.info("Getting all photos")
        val contentRoot = config.getConfig().mediaPath.let { File(it) }

    }

    override fun getFile(deviceId: Long, filename: String): FileSystemResource {
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

    fun getThumbFile(deviceId: Long, filename: String): FileSystemResource {
        // 1. Hent serialnummer fra DB
        val serial = withTransaction {
            DevicesTable.select(DevicesTable.serialNumber)
                .where { (DevicesTable.id eq deviceId) }
                .singleOrNull()?.get(DevicesTable.serialNumber)
        }.getOrNull() ?: throw IllegalArgumentException("Enhet med ID $deviceId finnes ikke")

        // 2. Konstruer stien til originalfilen
        val cachePath = config.getConfig().cachePath
        val originalFile = File(config.getConfig().mediaPath, "$serial/$filename")

        // 3. Sikkerhetssjekk: Eksisterer originalfilen?
        if (!originalFile.exists() || !originalFile.isFile) {
            throw FileNotFoundException("Filen $filename ble ikke funnet på enhet $serial")
        }

        val thumb = Thumbnail(File(cachePath, "$serial/thumbs"))
        return try {
            thumb.getThumbnailOf(originalFile)
        } catch (e: Exception) {
            log.error("Failed to generate thumbnail", e)
            log.warn("Prepare for large file!")
            FileSystemResource(originalFile)
        }
    }

    override fun getPagedFiles(page: Int, size: Int, serialNumber: String?): Pair<List<RemoteFile>, Long> {
        return withTransaction {
            // 1. Bygg opp listen med filtere dynamisk
            val conditions = mutableListOf<org.jetbrains.exposed.v1.core.Op<Boolean>>()

            // Kjernefilter: Kun bilder
            conditions.add(ImportedFilesTable.fileType eq FileType.IMAGE)

            // Siden vi kjører innerJoin, kan vi filtrere på varchar-kolonnen direkte i DevicesTable!
            if (serialNumber != null) {
                conditions.add(DevicesTable.serialNumber eq serialNumber)
            }

            // Kombiner alle filtere med AND
            val finalCondition = conditions.reduce { acc, op -> acc and op }

            // 2. Opprett base-spørringen (Join mellom tabellene)
            val baseQuery = ImportedFilesTable.innerJoin(DevicesTable)
                .select(ImportedFilesTable.id, ImportedFilesTable.deviceId, ImportedFilesTable.fileName)
                .where(finalCondition)

            // 3. Hent totalt antall treff basert på denne konfigurasjonen
            val total = baseQuery.count()

            // 4. Hent ut den paginerte dataen
            val data = ImportedFilesTable.innerJoin(DevicesTable)
                .select(ImportedFilesTable.id, ImportedFilesTable.deviceId, ImportedFilesTable.fileName)
                .where(finalCondition)
                .limit(size)
                .offset((page * size).toLong())
                .map { row ->
                    RemoteFile(
                        id = row[ImportedFilesTable.id].value,
                        deviceId = row[ImportedFilesTable.deviceId].value, // Dette gir ut Int-verdien trygt via .value
                        fileName = row[ImportedFilesTable.fileName]
                    )
                }

            Pair(data, total)
        }.getOrNull() ?: Pair(emptyList(), 0L)
    }
}