package no.iktdev.kammich.models.internal.events

import java.util.UUID

data class ImportJobClaimedEvent(val jobId: UUID, val userId: UUID)
