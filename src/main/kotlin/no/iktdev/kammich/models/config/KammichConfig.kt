package no.iktdev.kammich.models.config

data class KammichConfig(
    val cachePath: String,
    val apiKey: String? = null,
    val devices: List<DeviceConfig> = emptyList(),
)