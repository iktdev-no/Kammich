package no.iktdev.kammich.models.shared.config

data class DeviceConfig(
    val deviceId: String,          // Unik ID (serienummer)
    val sourcePath: List<String> = emptyList(), // Hva brukeren har valgt for denne
    val autoImport: Boolean = true
)