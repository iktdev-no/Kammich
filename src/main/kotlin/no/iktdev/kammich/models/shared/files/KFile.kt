package no.iktdev.kammich.models.shared.files

import no.iktdev.kammich.models.shared.storage.removable.Device

data class KFile(
    val id: String,
    val device: Device,          // Unik id (f.eks. sti for disk, eller gphoto-ID)
    val name: String,
    val type: KFileType,     // FILE, DIRECTORY
    val size: Long,
    val path: String,        // Den "tekniske" stien/identifikatoren
)

enum class KFileType { FILE, DIRECTORY }