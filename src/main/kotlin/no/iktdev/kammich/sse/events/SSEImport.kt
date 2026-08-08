package no.iktdev.kammich.sse.events

import no.iktdev.kammich.models.shared.DeviceImportSummary
import no.iktdev.kammich.models.shared.ImportProgressEvent
import no.iktdev.kammich.sse.ISSE

data class SSEImportMediaProgress(val payload: ImportProgressEvent): ISSE {
    override val type = "import-media-progress"
}

data class SSEImportState(val states: List<DeviceImportSummary>): ISSE {
    override val type = "import-device-state"
}