package no.iktdev.kammich.sse.events

import no.iktdev.kammich.models.shared.UploadProgressEvent
import no.iktdev.kammich.sse.ISSE

data class SSEUploadProgress(val payload: UploadProgressEvent): ISSE {
    override val type: String = "upload-media-progress"
}

