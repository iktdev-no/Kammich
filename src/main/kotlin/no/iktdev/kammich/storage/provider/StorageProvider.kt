package no.iktdev.kammich.storage.provider

import no.iktdev.kammich.models.internal.KFile
import no.iktdev.kammich.models.shared.device.RemovableDevice
import java.io.File

interface StorageProvider {
    fun listFiles(device: RemovableDevice, path: String? = null): List<KFile>
    fun listAllFiles(device: RemovableDevice, path: String?): List<KFile>
    fun getDCIM(device: RemovableDevice): KFile?
    fun copyFile(device: RemovableDevice, storeFile: File, importFile: KFile): File?

    fun deleteFile(device: RemovableDevice, file: KFile): Boolean
}