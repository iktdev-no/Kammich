package no.iktdev.kammich.sse

import no.iktdev.kammich.sse.events.SSEHeartbeat
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Component
class SseManager {

    private val emitters = CopyOnWriteArrayList<SseEmitter>()

    // Cache for å holde siste state per event-type eller unik nøkkel
    val eventCache = ConcurrentHashMap<String, ISSE>()

    fun createEmitter(): SseEmitter {
        val emitter = SseEmitter(0L)
        emitters.add(emitter)

        emitter.onCompletion { emitters.remove(emitter) }
        emitter.onTimeout { emitters.remove(emitter) }
        emitter.onError { emitters.remove(emitter) }

        return emitter
    }

    fun send(event: ISSE, cache: Boolean = true) {
        // Cache eventen basert på dens type (eller en mer spesifikk nøkkel hvis det gjelder spesifikke enheter)
        if (cache) {
            eventCache[event.type] = event
        }

        val deadEmitters = mutableListOf<SseEmitter>()

        emitters.forEach { emitter ->
            try {
                emitter.send(SseEmitter.event().data(event))
            } catch (ex: Exception) {
                deadEmitters.add(emitter)
            }
        }

        emitters.removeAll(deadEmitters)
    }

    @Scheduled(fixedDelay = 5_000)
    fun ping() {
        send(SSEHeartbeat(timestamp = System.currentTimeMillis()), false)
    }
}
