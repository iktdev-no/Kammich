package no.iktdev.kammich.database.models

data class PersistedImmichUser(
    val userId: String,
    val name: String,
    val email: String,
    val createdAt: String,
    val isActive: Boolean,
    val data: String,
)