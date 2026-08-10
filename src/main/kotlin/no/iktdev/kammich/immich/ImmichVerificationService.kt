package no.iktdev.kammich.immich

import no.iktdev.kammich.database.tables.ImmichAuthenticationTable
import no.iktdev.kammich.database.tables.ImmichUsersTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.models.shared.immich.ImmichAvailability
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.SSEImmichApiKeyInUse
import no.iktdev.kammich.sse.events.SSEImmichAvailability
import no.iktdev.kammich.sse.events.SSEImmichUser
import no.iktdev.kammich.util.gson
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class ImmichVerificationService(
    private val immichClientFactory: ImmichClientFactory,
    private val sseManager: SseManager
) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady(event: ApplicationReadyEvent) {
        verifyActiveConnectionAndNotify()
    }

    /**
     * Verifiserer den aktive tilkoblingen mot Immich-serveren,
     * henter oppdatert brukerinfo, og sender ut SSE-hendelser.
     */
    fun verifyActiveConnectionAndNotify(): Boolean {
        return withTransaction {
            val activeAuth = ImmichAuthenticationTable.selectAll()
                .where { ImmichAuthenticationTable.isActive eq true }
                .singleOrNull()

            if (activeAuth == null) {
                log.warn("Ingen aktiv Immich-server tilkobling funnet i databasen.")
                sseManager.send(SSEImmichAvailability(ImmichAvailability(
                    serverUrl = null,
                    isAvailable = false,
                    error = "Ingen aktiv tilkobling"
                )))
                return@withTransaction false
            }

            val serverUrl = activeAuth[ImmichAuthenticationTable.serverUrl]
            val apiKey = activeAuth[ImmichAuthenticationTable.apiKey]

            try {
                val client = immichClientFactory.create(serverUrl)

                val apiKeyInfo = client.getMyApiKey(apiKey)
                val userMe = client.meByApiKey(apiKey)

                // Send suksess
                sseManager.send(SSEImmichUser(userMe))
                sseManager.send(SSEImmichApiKeyInUse(apiKeyInfo))
                sseManager.send(SSEImmichAvailability(ImmichAvailability(
                    serverUrl = serverUrl,
                    isAvailable = true,
                    user = userMe
                )))

                true
            } catch (e: Exception) {
                log.error("Verifisering av Immich-tilkobling feilet for $serverUrl: ${e.message}", e)

                // Send feilstatus til frontenden slik at de vet at tilkoblingen feilet
                sseManager.send(SSEImmichAvailability(ImmichAvailability(
                    serverUrl = serverUrl,
                    isAvailable = false,
                    error = e.message ?: "Ukjent feil"
                )))

                false
            }
        }.getOrDefault(false)
    }
}