package no.iktdev.kammich.storage.provider

import no.iktdev.kammich.models.shared.device.BlockDevice
import no.iktdev.kammich.models.shared.device.GPhoto2Device
import no.iktdev.kammich.models.shared.device.RemovableDevice
import org.springframework.stereotype.Service

@Service
class StorageProviderFactory(
    private val gPhoto2StorageProvider: GPhoto2StorageProvider,
    private val blockStorageProvider: BlockStorageProvider
) {
    fun getProvider(device: RemovableDevice): StorageProvider {
        return when (device) {
            is GPhoto2Device -> gPhoto2StorageProvider
            is BlockDevice -> blockStorageProvider
            else -> throw IllegalArgumentException("Ingen provider for ${device.interfaceType}")
        }
    }
}