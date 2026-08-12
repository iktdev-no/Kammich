package no.iktdev.kammich.immich

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

    fun setCurrentUser(user: ImmichUserMe?) {
        activeUserRef.set(user)
        sseManager.send(SSEImmichUser(user))
    }

    fun getCurrentUser(): ImmichUserMe? {
        return activeUserRef.get()
    }

    // Praktisk snarvei for å få tak i selve UUID-en direkte
    fun getCurrentUserId(): UUID? {
        return activeUserRef.get()?.id
    }
}