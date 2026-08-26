package no.iktdev.kammich.models.internal.events

import java.util.UUID

data class ImportJobCompletedEvent(
    val jobId: UUID,
    val deviceSN: String,
)