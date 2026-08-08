package no.iktdev.kammich.models.shared.device

data class PhotoDevice(
    val name: String,
    val serialNumber: String,
    val manufacturer: String?,
    val model: String?,
)