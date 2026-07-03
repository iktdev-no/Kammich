package no.iktdev.kammich.gphoto2.model


enum class GPhoto2NodeType { FILE, FOLDER }

data class GPhoto2File(
    val name: String,
    val type: GPhoto2NodeType,
    val folderPath: String,
    val sizeBytes: Long = 0,
    val mimeType: String? = null
)