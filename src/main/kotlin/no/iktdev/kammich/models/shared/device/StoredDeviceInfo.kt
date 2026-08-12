package no.iktdev.kammich.models.shared.device

import no.iktdev.kammich.models.internal.PersistedDevice
import java.time.Instant

data class StoredDeviceInfo(
    val deviceName: String,
    val serialNumber: String,
    val manufacturer: String?,
    val model: String?,
    val deviceType: DeviceType,
    val lastSeen: Instant,
) {
    companion object {
        fun fromPersisted(device: PersistedDevice) = StoredDeviceInfo(
            deviceName = device.name,
            serialNumber = device.serialNumber,
            manufacturer = device.manufacturer,
            model = device.model,
            deviceType = device.deviceType?.let { DeviceType.valueOf(it) } ?: DeviceType.Unknown,
            lastSeen = Instant.parse(device.lastSeen),
        )
    }
}