package no.iktdev.kammich.models.storage

data class StorageInfo(
    val stats: StorageStats,
    val health: DiskHealth
) {

}