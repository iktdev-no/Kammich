package no.iktdev.kammich.database.models

data class PersistedImmichAuthentication(
    val userId: String,
    val apiKeyId: String,
    val serverUrl: String,
    val apiKey: String,
    val createdAt: String,
    val isActive: Boolean,
    val data: String,
)