package no.iktdev.kammich.models.shared

import no.iktdev.kammich.models.files.KFileType
import no.iktdev.kammich.models.shared.storage.removable.Device

data class WFile (
    val id: String,
    val name: String,
    val type: WFileType,     // FILE, DIRECTORY
    val size: Long,
    val path: String,        // Den "tekniske" stien/identifikatoren
    var importStatus: WFileStatus = WFileStatus.None,
    var uploaded: Boolean = false
)

enum class WFileType { FILE, DIRECTORY }
enum class WFileStatus { Included, Excluded, None }