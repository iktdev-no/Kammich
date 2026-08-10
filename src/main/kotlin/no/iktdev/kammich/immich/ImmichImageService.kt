package no.iktdev.kammich.immich

import no.iktdev.kammich.database.tables.ImmichAuthenticationTable
import no.iktdev.kammich.database.withTransaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ImmichImageService(
    private val immichClientFactory: ImmichClientFactory
) {

    // @Cacheable sørger for at resultatet lagres i minnet inntil applikasjonen restartes.
    // Vi bruker userId som nøkkel i cachen.
    @Cacheable("profileImages", key = "#userId")
    fun fetchProfileImageBytes(userId: UUID): ByteArray? {
        // Databasedelen og henting fra Immich
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

        val client = immichClientFactory.create(serverUrl)
        return client.getProfileImage(apiKey, userId)
    }
}