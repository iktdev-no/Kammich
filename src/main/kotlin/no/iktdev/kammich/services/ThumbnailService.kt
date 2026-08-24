package no.iktdev.kammich.services

import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.models.internal.events.ImportJobCompletedEvent
import no.iktdev.kammich.storage.Thumbnail
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileNotFoundException
import java.util.*

@Service
class ThumbnailService(
    private val configService: ConfigService, // Antatt navnet på config-tjenesten din
) {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        val cache = configService.getConfig().cachePath
        val folder = File(cache)
        if (!folder.exists()) {
            folder.mkdirs()
        }
    }

    fun getThumbFile(deviceId: Long, filename: String): FileSystemResource {
        // 1. Hent serialnummer fra DB
        val serial = withTransaction {
            DevicesTable.select(DevicesTable.serialNumber)
                .where { (DevicesTable.id eq deviceId) }
                .singleOrNull()?.get(DevicesTable.serialNumber)
        }.getOrNull() ?: throw IllegalArgumentException("Enhet med ID $deviceId finnes ikke")

        // 2. Konstruer stien til originalfilen
        val cachePath = configService.getConfig().cachePath
        val originalFile = File(configService.getConfig().mediaPath, "$serial/$filename")

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

    @EventListener
    fun onImportCompleted(event: ImportJobCompletedEvent) {
        log.info("Import fullført for jobb ${event.jobId} (Enhet: ${event.deviceId}). Starter generering av thumbnails...")

        try {
            // Konverter deviceId til Long hvis tabellen din krever det (eller slå opp direkte hvis den er String i DB)
            val serial = event.deviceId

            // 2. Finn mediamappen for enheten
            val mediaDir = File(configService.getConfig().mediaPath, serial)
            if (!mediaDir.exists() || !mediaDir.isDirectory) {
                log.warn("Mediamappe eksisterer ikke for enhet $serial på stien: ${mediaDir.absolutePath}")
                return
            }

            // 3. Initialiser Thumbnail-hjelperen for denne enheten
            val cachePath = configService.getConfig().cachePath
            val thumbHelper = Thumbnail(File(cachePath, "$serial/thumbs"))

            // 4. Gå gjennom alle filer i mappen og generer thumbnails preventivt
            val files = mediaDir.listFiles { file -> file.isFile } ?: emptyArray()
            var generatedCount = 0

            for (file in files) {
                try {
                    // Enkel sjekk på om det er et bilde (kan utvides ved behov)
                    if (isSupportedImage(file.name)) {
                        thumbHelper.generate(file)
                        generatedCount++
                    }
                } catch (e: Exception) {
                    log.error("Klarte ikke å generere thumbnail for ${file.name}", e)
                }
            }

            log.info("Ferdig med preventiv generering av thumbnails for enhet $serial. Generert $generatedCount thumbnails.")

        } catch (e: Exception) {
            log.error("Feil under asynkron generering av thumbnails for import-jobb ${event.jobId}", e)
        }
    }

    private fun isSupportedImage(filename: String): Boolean {
        val lower = filename.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".webp") ||
                lower.endsWith(".heic")
    }
}