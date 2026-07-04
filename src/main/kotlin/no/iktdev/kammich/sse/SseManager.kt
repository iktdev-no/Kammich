package no.iktdev.kammich.sse

import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CopyOnWriteArrayList

@Component
class SseManager {

    private val emitters = CopyOnWriteArrayList<SseEmitter>()

    fun createEmitter(): SseEmitter {
        val emitter = SseEmitter(0L)

        emitters.add(emitter)

        emitter.onCompletion { emitters.remove(emitter) }
        emitter.onTimeout { emitters.remove(emitter) }
        emitter.onError { emitters.remove(emitter) }

        return emitter
    }

    fun send(event: Any) {
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
}
