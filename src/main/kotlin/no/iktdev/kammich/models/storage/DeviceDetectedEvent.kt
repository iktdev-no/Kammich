package no.iktdev.kammich.models.storage

data class DeviceDetectedEvent(
    val sysPath: String,
    val vendor: String,
    val product: String,
    val serial: String,
    val gphotoPort: String,
    val isBlockDevice: Boolean
)