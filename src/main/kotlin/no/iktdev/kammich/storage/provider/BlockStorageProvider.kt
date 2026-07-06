package no.iktdev.kammich.storage.provider

import no.iktdev.kammich.models.shared.files.KFile
import no.iktdev.kammich.models.shared.storage.removable.Device
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

    override fun listAllFiles(
        device: Device,
        path: String?
    ): List<KFile> {
        TODO("Not yet implemented")
    }

    override fun getDCIM(device: Device): KFile? {
        TODO("Not yet implemented")
    }

    override fun getThumbnails(folder: KFile, recurse: Boolean): List<File> {
        TODO("Not yet implemented")
    }

    override fun getFile(
        device: Device,
        storeFile: File,
        importFile: KFile
    ): File? {
        TODO("Not yet implemented")
    }
}