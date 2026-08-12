package no.iktdev.kammich.storage.media

import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.ImportedFilesTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.models.FileType
import no.iktdev.kammich.models.shared.RemoteFile
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileNotFoundException

@Service
class VideoService(
    private val config: ConfigService,
): MediaService {
    private val log = LoggerFactory.getLogger(VideoService::class.java)

    override fun getFile(
        deviceId: Long,
        filename: String
    ): FileSystemResource {
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

    override fun getPagedFiles(page: Int, size: Int, serialNumber: String?): Pair<List<RemoteFile>, Long> {
        return withTransaction {
            // 1. Definer spørringen
            var query = ImportedFilesTable.innerJoin(DevicesTable)
                .selectAll()
                .where { ImportedFilesTable.fileType eq FileType.IMAGE }

            // 2. Legg til filter
            if (serialNumber != null) {
                query = query.andWhere { DevicesTable.serialNumber eq serialNumber }
            }

            // 3. Hent total (dette må gjøres før vi begrenser resultatet)
            val total = query.count()

            // 4. Kjed metodene hver for seg (dette er v1-stilen)
            val data = query
                .limit(size)
                .offset((page * size).toLong())
                .map { it ->
                    RemoteFile(
                        id = it[ImportedFilesTable.id].value,
                        deviceId = it[ImportedFilesTable.deviceId].value,
                        fileName = it[ImportedFilesTable.fileName],
                    )
                }

            Pair(data, total)
        }.getOrNull() ?: Pair(emptyList(), 0L)
    }

}