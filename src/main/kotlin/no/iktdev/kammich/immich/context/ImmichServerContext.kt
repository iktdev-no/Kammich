package no.iktdev.kammich.immich.context

import okhttp3.OkHttpClient
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Component
class ImmichServerContext(
    private val baseClient: OkHttpClient = OkHttpClient()
) {
    private val serverUrlRef = AtomicReference<String?>()
    private val isOnlineRef = AtomicBoolean(false)
    private val consecutiveFailures = AtomicInteger(0)

    companion object {
        const val MAX_FAILURES_BEFORE_OFFLINE = 3
    }

    fun setServerUrl(url: String?) {
        serverUrlRef.set(url?.trimEnd('/'))
        if (url == null) {
            resetState()
        }
    }

    fun getServerUrl(): String? {
        return serverUrlRef.get()
    }

    fun isOnline(): Boolean {
        return isOnlineRef.get()
    }

    fun getFailureCount(): Int {
        return consecutiveFailures.get()
    }

    /**
     * Kalles når en ping eller et API-kall lykkes.
     * Nullstiller feiltelleren og setter serveren online.
     */
    fun recordSuccess() {
        consecutiveFailures.set(0)
        val wasOffline = !isOnlineRef.get()
        isOnlineRef.set(true)

        if (wasOffline) {
            // Her kan du evt. sende SSE om at serveren er tilbake igjen om du ønsker
        }
    }

    /**
     * Kalles når en ping eller et API-kall feiler.
     * Øker feiltelleren og setter serveren offline hvis grensen nåes.
     */
    fun recordFailure(): Boolean {
        val failures = consecutiveFailures.incrementAndGet()

        if (failures >= MAX_FAILURES_BEFORE_OFFLINE) {
            val wasOnline = isOnlineRef.get()
            isOnlineRef.set(false)

            if (wasOnline) {
                // Her kan du logge at serveren nå er erklært offline pga. 3 feilede forsøk på rad
            }
        }

        return isOnlineRef.get() // Returnerer gjeldende helsestatus
    }

    fun clear() {
        resetState()
    }

    private fun resetState() {
        serverUrlRef.set(null)
        isOnlineRef.set(false)
        consecutiveFailures.set(0)
    }
}