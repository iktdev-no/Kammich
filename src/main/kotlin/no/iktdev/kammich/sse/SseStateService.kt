package no.iktdev.kammich.sse

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

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