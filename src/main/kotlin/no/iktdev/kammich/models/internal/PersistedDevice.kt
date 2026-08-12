package no.iktdev.kammich.models.internal

data class PersistedDevice(
    val id: Long, //Row id
    val name: String,
    val serialNumber: String,
    val manufacturer: String?,
    val model: String?,
    val deviceType: String?,
    val lastSeen: String,
)