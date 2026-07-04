package no.iktdev.kammich.controller

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.iktdev.kammich.sse.SseManager
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/sse")
class SseController(
    private val sseManager: SseManager
) {

    @GetMapping("/stream")
    fun stream(): SseEmitter {
        val emitter = sseManager.createEmitter()

        // Send første event (heartbeat)
        sseManager.send(mapOf(
            "type" to "ping",
            "timestamp" to System.currentTimeMillis()
        ))

        return emitter
    }
}

