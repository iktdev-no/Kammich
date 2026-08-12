package no.iktdev.kammich.models.shared

data class RemoteFile(
    val id: Long,
    val deviceId: Long,
    val fileName: String,
    val uploaded: Boolean = false
) {
}