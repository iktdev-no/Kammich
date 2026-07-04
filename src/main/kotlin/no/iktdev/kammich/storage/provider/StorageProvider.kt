package no.iktdev.kammich.storage.provider

import no.iktdev.kammich.models.files.KFile
import no.iktdev.kammich.models.storage.removable.Device
import java.io.File

interface StorageProvider {
    fun listFiles(device: Device, path: String? = null): List<KFile>
    fun getThumbnails(folder: KFile): List<File>
}