package no.iktdev.kammich.storage

import no.iktdev.kammich.models.shared.WFile
import no.iktdev.kammich.models.shared.device.RemovableDevice
import no.iktdev.kammich.storage.provider.StorageProviderFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FilesService(
    private val providerFactory: StorageProviderFactory
) {
    private val log = LoggerFactory.getLogger(FilesService::class.java)



    fun getFilesForDevice(device: RemovableDevice, path: String): List<WFile> {
        val provider = providerFactory.getProvider(device)
        return provider.listFiles(device, path).map { it.toWFile() }.also {
           // log.info("Returning:\n${it.joinToString("\n")}")
        }
    }
}