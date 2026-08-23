package no.iktdev.kammich.immich.services

import no.iktdev.kammich.services.ConfigService
import no.iktdev.kammich.database.tables.ImmichAuthenticationTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.immich.client.ImmichClientFactory
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@Service
class ImmichImageService(
    private val immichClientFactory: ImmichClientFactory,
    private val config: ConfigService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val profilesCacheDir: Path by lazy {
        Path.of(config.getConfig().cachePath, "profiles").also {
            Files.createDirectories(it)
        }
    }

    fun fetchProfileImageBytes(userId: UUID): ByteArray? {
        val file = profilesCacheDir.resolve("$userId.jpg")

        // 1. Sjekk disk-cachen først
        if (Files.exists(file)) {
            try {
                val bytes = Files.readAllBytes(file)
                // Hvis den lagrede fila er en tom placeholder (eller faktisk bilde), returner den
                return if (bytes.isNotEmpty()) bytes else null
            } catch (e: Exception) {
                log.warn("Failed to read cached profile image from disk: ${e.message}")
            }
        }

        // 2. Hent fra database
        val auth = withTransaction {
            ImmichAuthenticationTable.selectAll()
                .where { ImmichAuthenticationTable.userId eq userId.toString() and (ImmichAuthenticationTable.isActive eq true) }
                .singleOrNull() ?: ImmichAuthenticationTable.selectAll()
                .where { ImmichAuthenticationTable.userId eq userId.toString() }
                .limit(1)
                .singleOrNull()
        }.getOrNull() ?: return null

        val serverUrl = auth[ImmichAuthenticationTable.serverUrl]
        val apiKey = auth[ImmichAuthenticationTable.apiKey]

        // 3. Hent fra Immich
        val client = immichClientFactory.create(serverUrl)
        val imageBytes = client.getProfileImage(apiKey, userId)

        // 4. Skriv til disk (selv om den er null, kan vi evt skrive en tom fil eller unngå det for å prøve igjen senere.
        // Her velger vi å lagre hvis vi fikk data, slik at vi slipper nettverkskall neste gang).
        if (imageBytes != null && imageBytes.isNotEmpty()) {
            try {
                Files.write(file, imageBytes)
            } catch (e: Exception) {
                log.warn("Failed to write profile image cache to disk: ${e.message}")
            }
        }

        return imageBytes
    }
}