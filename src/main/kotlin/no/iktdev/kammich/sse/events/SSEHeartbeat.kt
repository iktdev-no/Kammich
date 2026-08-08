package no.iktdev.kammich.sse.events

import no.iktdev.kammich.sse.ISSE

data class SSEHeartbeat(val timestamp: Long): ISSE {
    override val type: String = "ping"
}