package no.iktdev.kammich.models.shared.storage.removable

import no.iktdev.kammich.models.shared.storage.DeviceType

data class Device(
    val id: String, // Serialnumber
    val name: String,
    val type: DeviceType,
    val path: String, // GPhoto2 path or mount paht
    val vendor: String? = null,
    val model: String? = null,
)
