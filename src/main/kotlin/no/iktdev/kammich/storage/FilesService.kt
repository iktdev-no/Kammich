package no.iktdev.kammich.storage

import no.iktdev.kammich.models.shared.files.KFile
import no.iktdev.kammich.models.shared.storage.removable.Device
import no.iktdev.kammich.storage.provider.BlockStorageProvider
import no.iktdev.kammich.storage.provider.StorageProviderFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FilesService(
    private val providerFactory: StorageProviderFactory
) {
    private val log = LoggerFactory.getLogger(FilesService::class.java)



    fun getFilesForDevice(device: Device, path: String): List<KFile> {
        log.info("Getting files for device ${device.id} on path $path")
        val provider = providerFactory.getProvider(device)
        return provider.listFiles(device, path).also {
           // log.info("Returning:\n${it.joinToString("\n")}")
        }
    }
}