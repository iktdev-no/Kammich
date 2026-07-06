package no.iktdev.kammich.models.shared.storage.removable

import no.iktdev.kammich.models.shared.storage.DeviceType

data class DeviceInfo(
    val id: String,
    val type: DeviceType,
    val friendlyName: String?,
    val manufacturer: String?,
    val model: String?,
    val capabilities: List<Capability>, // Liste fremfor bool-felter
    val storage: List<DeviceStorageStats>,
    val attributes: Map<String, Any> = emptyMap() // "Alt det andre"
)

enum class Capability {
    CAPTURE, DELETE, UPLOAD, PREVIEW, CONFIGURE
}

data class DeviceStorageStats(
    val id: String,
    val description: String,
    val capacityBytes: Long,
    val freeSpaceBytes: Long
)