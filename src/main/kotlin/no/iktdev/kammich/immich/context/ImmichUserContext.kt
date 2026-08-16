package no.iktdev.kammich.immich.context

import no.iktdev.kammich.models.shared.immich.api.ImmichUserMe
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.SSEImmichUser
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

@Component
class ImmichUserContext(
    private val sseManager: SseManager
) {

    private val activeUserRef = AtomicReference<ImmichUserMe?>()
    private val activeUserApiKeyRef = AtomicReference<String>()

    fun setCurrentUser(user: ImmichUserMe, apiKey: String) {
        activeUserRef.set(user)
        activeUserApiKeyRef.set(apiKey)
        sseManager.send(SSEImmichUser(user))
    }

    fun getCurrentUser(): ImmichUserMe? {
        return activeUserRef.get()
    }

    fun getCurrentUserApiKey(): String? {
        return activeUserApiKeyRef.get()
    }

    // Praktisk snarvei for å få tak i selve UUID-en direkte
    fun getCurrentUserId(): UUID? {
        return activeUserRef.get()?.id
    }

    fun clear() {
        activeUserRef.set(null)
        activeUserApiKeyRef.set(null)
        sseManager.send(SSEImmichUser(null))
    }
}