package no.iktdev.kammich.models.shared

import java.time.Instant


data class AlbumCreateRequest(
    val albumName: String,
    val description: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
)

data class AlbumUpdateRequest(
    val albumName: String?,
    val description: String?,
    val startDate: String?,
    val endDate: String?,
    val use: Boolean?
)

data class Album(
    val id: Long,
    val title: String,
    val description: String? = null,
    val startDate: Instant? = null,
    val endDate: Instant? = null,
    val use: Boolean,
    val createdAt: String,
    val totalFiles: Long = 0,
    val sampleFile: RemoteFile? = null
)