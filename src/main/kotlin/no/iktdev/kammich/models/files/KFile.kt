package no.iktdev.kammich.models.files

import no.iktdev.kammich.models.shared.WFile
import no.iktdev.kammich.models.shared.WFileType
import no.iktdev.kammich.models.shared.storage.removable.Device

data class KFile(
    val id: String,
    val device: Device,          // Unik id (f.eks. sti for disk, eller gphoto-ID)
    val name: String,
    val type: KFileType,     // FILE, DIRECTORY
    val size: Long,
    val path: String,        // Den "tekniske" stien/identifikatoren
) {
    fun toWFile() = WFile(
        id = id,
        name = name,
        type = WFileType.valueOf(type.name),
        size = size,
        path = path,
    )
}

enum class KFileType { FILE, DIRECTORY }