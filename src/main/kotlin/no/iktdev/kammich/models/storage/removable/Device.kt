package no.iktdev.kammich.models.storage.removable

import no.iktdev.kammich.models.storage.DeviceType

data class Device(
    val id: String,
    val name: String,
    val type: DeviceType,
    val path: String? = null, // GPhoto2 path or mount paht
    val vendor: String? = null,
    val model: String? = null,
)
