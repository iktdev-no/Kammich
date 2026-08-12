package no.iktdev.kammich.models.internal.events

import no.iktdev.kammich.database.models.PersistedImportedFile
import java.util.UUID

data class ImportJobCompletedEvent(
    val jobId: UUID,
    val deviceId: String,
)