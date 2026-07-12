package no.iktdev.kammich.models.shared.config

import no.iktdev.kammich.models.immich.auth.ImmichAuth

data class KammichConfig(
    val mediaPath: String,
    val apiAuth: ImmichAuth? = null,
    val assignUnknownDeviceAsBlockDevice: Boolean = false,
    val autoImportCameraByDefault: Boolean = true,
    val deviceSettings: MutableMap<String, DeviceSettings> = mutableMapOf()
)