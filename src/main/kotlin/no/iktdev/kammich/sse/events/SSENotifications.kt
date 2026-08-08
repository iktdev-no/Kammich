package no.iktdev.kammich.sse.events

import no.iktdev.kammich.models.shared.Notification
import no.iktdev.kammich.sse.ISSE

data class SSENotifications(val payload: List<Notification>): ISSE {
    override val type: String = "notifications"
}