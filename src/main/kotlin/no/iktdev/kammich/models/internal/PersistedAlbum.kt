package no.iktdev.kammich.models.internal

import no.iktdev.kammich.models.shared.Album
import no.iktdev.kammich.models.shared.RemoteFile
import java.time.Instant
import java.util.UUID

data class PersistedAlbum(
    val id: Long,
    val name: String,
    val immichUserId: UUID,
    val description: String? = null,
    val startDate: Instant? = null,
    val endDate: Instant? = null,
    val use: Boolean = false,
    val createdAt: Instant,
) {
    fun toAlbum(fileCount: Long, sampleFile: RemoteFile? = null): Album {
        return Album(
            id = id,
            title = name,
            description = description,
            startDate = startDate,
            endDate = endDate,
            createdAt = createdAt.toString(),
            use = use,
            totalFiles = fileCount,
            sampleFile = sampleFile
        )
    }
}