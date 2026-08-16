package no.iktdev.kammich.immich.services

import com.google.gson.Gson // Eventuell avhengighet for gson, tilpass om du har den injisert
import no.iktdev.kammich.database.tables.ImmichAuthenticationTable
import no.iktdev.kammich.database.tables.ImmichUsersTable
import no.iktdev.kammich.database.tables.ImmichUsersTable.toPersistedImmichUser
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.immich.ImmichApi
import no.iktdev.kammich.immich.ImmichRepository
import no.iktdev.kammich.immich.client.ImmichClientFactory
import no.iktdev.kammich.immich.context.ImmichServerContext
import no.iktdev.kammich.immich.context.ImmichUserContext
import no.iktdev.kammich.models.shared.immich.ImmichAvailability
import no.iktdev.kammich.models.shared.immich.api.ImmichUserMe
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.SSEImmichApiKeyInUse
import no.iktdev.kammich.sse.events.SSEImmichAvailability
import no.iktdev.kammich.util.gson
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ImmichContextService(
    private val immichClientFactory: ImmichClientFactory,
    private val immichServerContext: ImmichServerContext,
    private val immichUserContext: ImmichUserContext,
    private val immichRepository: ImmichRepository,
    private val immichVerificationService: ImmichVerificationService,
    private val sseManager: SseManager,
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    data class SavedSession(
        val serverUrl: String,
        val apiKey: String,
        val user: ImmichUserMe
    )

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        initializeAndVerifyContext()
    }

    fun initializeAndVerifyContext(): Boolean {
        val session = findActiveSessionInDb() ?: run {
            handleNoActiveSession("Ingen aktiv tilkobling")
            return false
        }

        // 1. Sett både server-url og den lagrede brukeren + API-nøkkel med en gang,
        // slik at kontexten har alt den trenger selv om serveren er nede.
        immichServerContext.setServerUrl(session.serverUrl)
        immichUserContext.setCurrentUser(session.user, session.apiKey)

        val client = immichClientFactory.create(session.serverUrl)

        // 2. Sjekk om serveren svarer
        if (!isServerReachable(client)) {
            handleServerUnreachable(session.serverUrl, session.user, "Serveren svarer ikke (utilgjengelig)")
            return false
        }

        // 3. Prøv å verifisere mot live server og oppdatere med ferske data om alt er ok
        return try {
            verifyAndLoadUserData(client, session)
            true
        } catch (e: Exception) {
            handleVerificationError(session.serverUrl, session.user, e)
            false
        }
    }

    private fun findActiveSessionInDb(): SavedSession? {
        return withTransaction {
            // Hent aktiv auth-sesjon (serverUrl og apiKey)
            val authRow = ImmichAuthenticationTable.selectAll()
                .where { ImmichAuthenticationTable.isActive eq true }
                .singleOrNull() ?: return@withTransaction null

            val serverUrl = authRow[ImmichAuthenticationTable.serverUrl]
            val apiKey = authRow[ImmichAuthenticationTable.apiKey]

            // Hent aktiv bruker ved hjelp av din metode (tilpasset inn i transaksjonen)
            val pusr = ImmichUsersTable.selectAll()
                .where { ImmichUsersTable.isActive eq true }
                .map { it.toPersistedImmichUser() }
                .singleOrNull() ?: return@withTransaction null

            val user = gson.fromJson(pusr.data, ImmichUserMe::class.java) ?: return@withTransaction null

            SavedSession(
                serverUrl = serverUrl,
                apiKey = apiKey,
                user = user
            )
        }.getOrNull()
    }

    private fun isServerReachable(client: ImmichApi): Boolean {
        val success = client.getServerPing()
        if (success) {
            immichServerContext.recordSuccess()
        } else {
            immichServerContext.recordFailure()
        }
        return success
    }

    private fun verifyAndLoadUserData(client: ImmichApi, session: SavedSession) {
        val apiKeyInfo = client.getMyApiKey(session.apiKey)
        val userMe = client.meByApiKey(session.apiKey)

        if (apiKeyInfo == null || userMe == null) {
            throw IllegalStateException("API-nøkkelen eller brukeren ble avvist av serveren.")
        }

        // Serveren svarer og alt er verifisert OK -> Oppdater til frisk og sett ferske data
        immichServerContext.recordSuccess()
        immichUserContext.setCurrentUser(userMe, session.apiKey)

        sseManager.send(SSEImmichApiKeyInUse(apiKeyInfo))
        sseManager.send(
            SSEImmichAvailability(
                ImmichAvailability(
                    serverUrl = session.serverUrl,
                    isAvailable = true,
                    user = userMe
                )
            )
        )

        log.info("Vellykket verifisering av Immich-sesjon for ${userMe.name}")
    }

    private fun handleServerUnreachable(serverUrl: String, user: ImmichUserMe, reason: String) {
        log.warn("Fant lagret sesjon for $serverUrl, men serveren svarer ikke. Bruker ${user.name} beholdes i kontext.")
        immichServerContext.recordFailure()
        notifyUnavailable(serverUrl, user, reason)
    }

    private fun handleVerificationError(serverUrl: String, user: ImmichUserMe, e: Exception) {
        log.error("Feil under verifisering av Immich-tilkobling mot $serverUrl: ${e.message}. Bruker beholdes i kontext.")
        immichServerContext.recordFailure()
        notifyUnavailable(serverUrl, user, e.message ?: "Nettverksfeil")
    }

    private fun notifyUnavailable(serverUrl: String, user: ImmichUserMe, errorReason: String) {
        sseManager.send(
            SSEImmichAvailability(
                ImmichAvailability(
                    serverUrl = serverUrl,
                    isAvailable = false,
                    user = user, // Sender med den lagrede brukeren selv om serveren er nede
                    error = errorReason
                )
            )
        )
    }

    private fun handleNoActiveSession(errorReason: String) {
        log.info("Ingen aktiv Immich-sesjon funnet i databasen ved oppstart.")
        immichServerContext.clear()
        immichUserContext.clear()
        sseManager.send(
            SSEImmichAvailability(
                ImmichAvailability(
                    serverUrl = null,
                    isAvailable = false,
                    user = null,
                    error = errorReason
                )
            )
        )
    }


    fun switchUser(userId: UUID): Boolean {
        val repoUpdated = immichRepository.switchUser(userId.toString())
        if (!repoUpdated) {
            log.warn("Klarte ikke å bytte bruker til $userId i databasen.")
            return false
        }

        // 2. Gjenbruk verifiseringslogikken din for å hente ut ferske data,
        // oppdatere minne-kontekster og trigge SSE-hendelser automatisk!
        val verified = immichVerificationService.verifyActiveConnectionAndNotify()

        if (!verified) {
            log.error("Bruker ble satt til aktiv i DB, men verifisering feilet for $userId.")
            // Valgfritt: Rull tilbake eller la den stå som "utilgjengelig" inntil videre
        }

        return verified
    }
}