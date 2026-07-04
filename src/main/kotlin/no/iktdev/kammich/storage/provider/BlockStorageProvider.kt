package no.iktdev.kammich.storage.provider

import no.iktdev.kammich.models.files.KFile
import no.iktdev.kammich.models.storage.removable.Device
import org.springframework.stereotype.Service
import java.io.File

@Service
class BlockStorageProvider: StorageProvider {

    override fun listFiles(
        device: Device,
        path: String?
    ): List<KFile> {
        TODO("Not yet implemented")
    }

    override fun getThumbnails(folder: KFile): List<File> {
        TODO("Not yet implemented")
    }
}