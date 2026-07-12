package no.iktdev.kammich.models.internal

interface ConnectedDevice {
}

data class GPhotoDevice(
    val port: String
): ConnectedDevice {}

data class BlockDevice(
    val mountPoint: String,
): ConnectedDevice