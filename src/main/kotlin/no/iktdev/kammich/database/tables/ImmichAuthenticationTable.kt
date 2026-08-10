package no.iktdev.kammich.database.tables

import no.iktdev.kammich.database.models.PersistedImmichAuthentication
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object ImmichAuthenticationTable : IntIdTable("IMMICH_API_KEY") {
    val userId = varchar("USER_ID", 36)
    val apiKeyId = varchar("API_KEY_ID", 36)
    val serverUrl = text("SERVER_URL")
    val apiKey = text("API_KEY")
    val createdAt = text("CREATED_AT")
    val isActive = bool("IS_ACTIVE")
    val data = text("DATA")

    fun ResultRow.toPersistedApiKey(): PersistedImmichAuthentication {
        return PersistedImmichAuthentication(
            userId = this[userId],
            apiKeyId = this[apiKeyId],
            serverUrl = this[serverUrl],
            apiKey = this[apiKey],
            createdAt = this[createdAt],
            isActive = this[isActive],
            data = this[data],
        )
    }
}