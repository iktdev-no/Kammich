package no.iktdev.kammich.services

import no.iktdev.kammich.models.NotificationDismissed
import no.iktdev.kammich.models.shared.Notification
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.SseStateService
import no.iktdev.kammich.sse.events.SSENotifications
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class NotificationService(
    private val sseManager: SseManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val notifications = ConcurrentHashMap<String, Notification>()

    @EventListener
    fun handleNotificationCreatedEvent(event: Notification) {
        log.info("Received notification: $event")
        notifications[event.id] = event
        sseManager.send(notificationPayload())
    }

    @EventListener
    fun handleNotificationDismissedEvent(event: NotificationDismissed) {
        log.info("Received notification: $event")
        notifications.remove(event.id)
        sseManager.send(notificationPayload())
    }

    fun dismiss(id: String): Boolean {
        val result = notifications.remove(id)
        sseManager.send(notificationPayload())
        return result != null
    }

    fun dismissAll(): Int {
        // Valgfritt: Hvis du bare vil slette de som faktisk er dismissable
        val dismissableIds = notifications.values
            .filter { it.dismissable }
            .map { it.id }

        dismissableIds.forEach { notifications.remove(it) }

        // Send oppdatert liste via SSE
        sseManager.send(notificationPayload())

        log.info("Dismissed ${dismissableIds.size} notifications")
        return dismissableIds.size
    }

    fun notificationPayload() = SSENotifications(
        notifications.values.toList()
    )
}