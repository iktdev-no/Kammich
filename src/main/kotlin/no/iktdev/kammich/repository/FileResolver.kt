package no.iktdev.kammich.repository

import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.ImportedFilesTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.services.ConfigService
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Component
import java.io.File

@Component
class FileResolver(
    private val configService: ConfigService
) {
    fun resolve(fileId: Long): File? {
        val file = withTransaction {
            ImportedFilesTable.getWhere { ImportedFilesTable.id eq fileId }
                .firstOrNull()
        }.getOrThrow() ?: return null

        val serial = withTransaction {
            DevicesTable
                .select(DevicesTable.serialNumber)
                .where { DevicesTable.id eq file.deviceId }
                .firstOrNull()
        }.getOrThrow()?.get(DevicesTable.serialNumber) ?: return null

        return file.getFile(configService.getConfig().mediaPath, serial)
            .takeIf { it.exists() }
    }

    fun resolve(fileIds: List<Long>): Map<Long, File> {
        if (fileIds.isEmpty()) return emptyMap()

        val files = withTransaction {
            ImportedFilesTable.getWhere { ImportedFilesTable.id inList fileIds }
        }.getOrThrow()

        val devices = withTransaction {
            DevicesTable
                .select(DevicesTable.id, DevicesTable.serialNumber)
                .where { DevicesTable.id inList files.map { it.deviceId }.distinct() }
                .associate { it[DevicesTable.id].value to it[DevicesTable.serialNumber] }
        }.getOrThrow()

        val mediaPath = configService.getConfig().mediaPath

        return files.associate { file ->
            val serial = devices[file.deviceId]
                ?: throw IllegalStateException("Serial number not found for device ID: ${file.deviceId}")
            file.id to file.getFile(mediaPath, serial)
        }
    }
}