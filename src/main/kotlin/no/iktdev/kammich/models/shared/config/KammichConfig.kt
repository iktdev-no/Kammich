package no.iktdev.kammich.models.shared.config

import no.iktdev.kammich.models.DeviceSettings
import no.iktdev.kammich.models.immich.auth.ImmichAuth

data class KammichConfig(
    val mediaPath: String,
    val apiAuth: ImmichAuth? = null,
    val autoImportCameraByDefault: Boolean = true,
    val deviceSettings: MutableMap<String, DeviceSettings> = mutableMapOf()
)