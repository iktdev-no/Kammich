package no.iktdev.kammich.gphoto2.model

data class GPhoto2Summary(
    val manufacturer: String?,
    val model: String?,
    val serialNumber: String?,
    val batteryLevel: Int?, // Nullable
    val friendlyDeviceName: String?,
    val storageDevices: List<GPhoto2StorageDevice> // Denne kan forbli en tom liste, det er tryggere
)

data class GPhoto2StorageDevice(
    val id: String,
    val description: String,
    val capacityBytes: Long,
    val freeSpaceBytes: Long
)