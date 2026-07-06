package no.iktdev.kammich.models.shared

data class RemoteFile(
    val id: Int,
    val deviceId: Int,
    val fileName: String,
) {
}