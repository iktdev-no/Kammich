package no.iktdev.kammich.database.models

import java.util.UUID

data class PersistedJobOwner(
    val jobId: UUID,
    val immichUserId: UUID,
)