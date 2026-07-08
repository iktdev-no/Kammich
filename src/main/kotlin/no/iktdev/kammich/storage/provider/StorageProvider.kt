package no.iktdev.kammich.storage.provider

import no.iktdev.kammich.models.files.KFile
import no.iktdev.kammich.models.shared.storage.removable.Device
import java.io.File

interface StorageProvider {
    fun listFiles(device: Device, path: String? = null): List<KFile>
    fun listAllFiles(device: Device, path: String?): List<KFile>
    fun getDCIM(device: Device): KFile?
    fun copyFile(device: Device, storeFile: File, importFile: KFile): File?
}