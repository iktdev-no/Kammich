package no.iktdev.kammich.sse.events

import no.iktdev.kammich.models.shared.storage.DiskHealth
import no.iktdev.kammich.models.shared.storage.MediaStats
import no.iktdev.kammich.models.shared.storage.StorageInfo
import no.iktdev.kammich.sse.ISSE

data class SSEStorageStatsMedia(val payload: MediaStats): ISSE {
    override val type = "storage-stats-media"
}

data class SSEStorageInfoInternal(val payload: List<StorageInfo>): ISSE {
    override val type = "storage-info-internal"
}

data class SSEStorageHealth(val payload: List<DiskHealth>): ISSE {
    override val type = "storage-health"
}