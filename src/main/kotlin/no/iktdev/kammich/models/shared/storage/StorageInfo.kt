package no.iktdev.kammich.models.shared.storage

data class StorageInfo(
    val stats: StorageStats,
    val health: DiskHealth
) {

}