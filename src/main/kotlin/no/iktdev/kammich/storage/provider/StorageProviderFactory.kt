package no.iktdev.kammich.storage.provider

import no.iktdev.kammich.models.shared.storage.DeviceType
import no.iktdev.kammich.models.shared.storage.removable.Device
import org.springframework.stereotype.Service

@Service
class StorageProviderFactory(
    private val gPhoto2StorageProvider: GPhoto2StorageProvider,
    private val blockStorageProvider: BlockStorageProvider
) {
    fun getProvider(device: Device): StorageProvider {
        return when (device.type) {
            DeviceType.PTP, DeviceType.MTP -> gPhoto2StorageProvider
            DeviceType.BLOCK -> blockStorageProvider
            else -> throw IllegalArgumentException("Ingen provider for ${device.type}")
        }
    }
}