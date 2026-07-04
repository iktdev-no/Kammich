package no.iktdev.kammich.sse

import no.iktdev.kammich.storage.DeviceManagerService
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Service
class SseStateService(
    private val deviceManagerService: DeviceManagerService,
) {
    fun sendCurrentState(emitter: SseEmitter) {
        emitter.send(deviceManagerService.ssePayload())
    }


}