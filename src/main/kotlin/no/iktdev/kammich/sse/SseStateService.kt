package no.iktdev.kammich.sse

import no.iktdev.kammich.models.NotificationDismissed
import no.iktdev.kammich.models.shared.Notification
import no.iktdev.kammich.sse.events.SSENotifications
import no.iktdev.kammich.storage.DeviceManagerService
import no.iktdev.kammich.storage.internal.DiskStorageService
import no.iktdev.kammich.storage.internal.StorageInfoPublisher
import no.iktdev.kammich.system.network.wifi.WifiConnectivityService
import no.iktdev.kammich.system.network.wifi.WifiScanner
import no.iktdev.kammich.system.network.wifi.WifiTetherService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Service
class SseStateService(
    private val sseManager: SseManager,
) {
    private val log = LoggerFactory.getLogger(SseStateService::class.java)

    fun sendCurrentState(emitter: SseEmitter) {
        /*emitter.send(deviceManagerService.ssePayload())
        emitter.send(notificationPayload())
        emitter.send(storageService.getPayload())
        emitter.send(dss.getPayload())
        emitter.send(wifiScanner.getSSEPayload())
        emitter.send(wifiConnectivityService.getSSEPayload())
        emitter.send(wifiTetherService.getSSETetheringPayload())*/
        // Send cached state med en gang klienten kobler til

        sseManager.eventCache.forEach { (_, event) ->
            sseManager.send(event, false)
        }


    }

}