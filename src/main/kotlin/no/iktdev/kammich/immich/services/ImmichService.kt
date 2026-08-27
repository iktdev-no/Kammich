package no.iktdev.kammich.immich.services

import no.iktdev.kammich.database.tables.ImmichAuthenticationTable
import no.iktdev.kammich.database.tables.ImmichUsersTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.immich.ImmichRepository
import no.iktdev.kammich.immich.client.ImmichClientFactory
import no.iktdev.kammich.immich.context.ImmichServerContext
import no.iktdev.kammich.immich.context.ImmichUserContext
import no.iktdev.kammich.models.shared.immich.ImmichLoginRequest
import no.iktdev.kammich.models.shared.immich.ImmichUserAccesses
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPost
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPostResponse
import no.iktdev.kammich.models.shared.immich.api.ImmichServerConfig
import no.iktdev.kammich.models.shared.immich.api.ImmichServerFeatures
import no.iktdev.kammich.models.shared.immich.api.ImmichServerStorage
import no.iktdev.kammich.models.shared.immich.api.ImmichServerVersion
import no.iktdev.kammich.models.shared.immich.api.ImmichSupportedMediaTypes
import no.iktdev.kammich.models.shared.immich.api.ImmichUserMe
import no.iktdev.kammich.models.shared.immich.api.defaultPermissions
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.SSEImmichApiKeyInUse
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ImmichService(
    private val immichClientFactory: ImmichClientFactory,
    private val immichVerificationService: ImmichVerificationService,
    private val immichUserContext: ImmichUserContext,
    private val immichServerContext: ImmichServerContext,
    private val immichRepository: ImmichRepository,
    private val immichContextService: ImmichContextService,
    private val sseManager: SseManager
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    fun logout() {
        // Implementer utlogging: Deaktiver aktiv bruker/nøkkel i db, tøm context, send SSE
        withTransaction {
            ImmichUsersTable.update({ ImmichUsersTable.isActive eq true }) { it[isActive] = false }
            ImmichAuthenticationTable.update({ ImmichAuthenticationTable.isActive eq true }) { it[isActive] = false }
        }
        immichUserContext.clear()
        sseManager.send(SSEImmichApiKeyInUse(null)) // Eller tilsvarende
        immichServerContext.clear()
        log.info("Logged out current Immich user")
    }

    fun switchUser(userId: UUID): Boolean {
        val success = immichContextService.switchUser(userId)
        if (success) {
            // Oppdater kontekst og send SSE om ny bruker
            log.info("Switched active Immich user to $userId")
        }
        return success
    }



    fun login(payload: ImmichLoginRequest): ImmichUserMe? {
        val baseUrl = payload.address.trim().removeSuffix("/")
        val client = immichClientFactory.create(baseUrl)

        val loginResult = try {
            client.login(payload.toImmichUserLogin())
        } catch (e: Exception) {
            log.error("Failed to login user", e)
            throw e
        }

        val userIdStr = loginResult.userId.toString()
        log.info("Logged in to Immich user $userIdStr")

        // 1. Prøv å finne en som allerede finnes og virker
        // (Vi returnerer et par med både nøkkel og id/detaljer her for enklere håndtering)
        val existingKey = findUsableApiKeyInfoFor(loginResult.userId, baseUrl)
        if (existingKey == null) {
            log.info("User $userIdStr has no api key")
        }

        val apiKeyResponse = if (existingKey != null) {
            existingKey
        } else {
            // 2. Hvis ikke, lag ny
            val newKeyResponse = createNewApiKey(baseUrl, loginResult.accessToken)
            // Prøv å rydde opp web-sesjonen pent
            try { client.logout(loginResult.accessToken) } catch (_: Exception) {

            }
            newKeyResponse
        }

        log.info("Api key response: $apiKeyResponse")

        val me = client.meByApiKey(apiKeyResponse.secret)

        // Lagre via repository med rette variabler
        immichRepository.setActiveUserAndServer(
            userIdStr = userIdStr,
            serverUrl = baseUrl,
            apiKeyId = apiKeyResponse.apiKey.id.toString(),
            secret = apiKeyResponse.secret,
            apiKeyDto = apiKeyResponse,
            me = me
        )

        immichServerContext.setServerUrl(baseUrl)
        immichUserContext.setCurrentUser(me, apiKeyResponse.secret)
        sseManager.send(SSEImmichApiKeyInUse(apiKeyResponse.apiKey))
        immichVerificationService.verifyActiveConnectionAndNotify()
        return me
    }

    private fun findUsableApiKeyInfoFor(user: UUID, baseUrl: String): ImmichApiKeyPostResponse? {
        val client = immichClientFactory.create(baseUrl)
        val keys = immichRepository.findApiKeysFor(user.toString(), baseUrl)

        for (key in keys) {
            try {
                // Sjekk om nøkkelen fortsatt lever på serveren
                val remoteKeyDto = client.getMyApiKey(key.apiKey)
                if (remoteKeyDto != null) {
                    // Returner i samme format som createApiKey for enkelhet skyld
                    return ImmichApiKeyPostResponse(
                        apiKey = remoteKeyDto,
                        secret = key.apiKey
                    )
                }
            } catch (e: Exception) {
                log.debug("Found dead API key in DB, skipping: ${e.message}")
            }
        }
        return null
    }

    private fun createNewApiKey(url: String, accessToken: String): ImmichApiKeyPostResponse {
        val immichClient = immichClientFactory.create(url)

        val apiKeyPayload = ImmichApiKeyPost(
            name = "Kammich Ingest Service",
            permissions = defaultPermissions
        )

        return immichClient.createApiKey(accessToken, apiKeyPayload)
    }


    fun getUsersWithAccesses(): List<ImmichUserAccesses> {
        return immichRepository.getAllUsersWithAccesses()
    }

    fun getUsers(): List<ImmichUserMe> {
        return immichRepository.getUsers()
    }

    fun deleteApiKey(apiKeyId: String): ResponseEntity<Void> {
        val deletedRows = immichRepository.deleteApiKey(apiKeyId)
        return when {
            deletedRows == 1 -> ResponseEntity.noContent().build()
            deletedRows > 1 -> error("Slettet $deletedRows rader for apiKeyId=$apiKeyId, forventet nøyaktig 1.")
            else -> ResponseEntity.notFound().build()
        }
    }

    fun getServerVersion(): ImmichServerVersion? {
        val url = immichServerContext.getServerUrl() ?: return null
        return immichClientFactory.create(url)
            .getServerVersion()
    }

    fun getServerSupportedMediaTypes(): ImmichSupportedMediaTypes? {
        val url = immichServerContext.getServerUrl() ?: return null
        return immichClientFactory.create(url)
            .getServerSupportedMediaTypes()
    }

    fun getServerFeatures(): ImmichServerFeatures? {
        val url = immichServerContext.getServerUrl() ?: return null
        return immichClientFactory.create(url)
            .getServerFeatures()
    }

    fun getServerConfig(): ImmichServerConfig? {
        val url = immichServerContext.getServerUrl() ?: return null
        return immichClientFactory.create(url)
            .getServerConfig()
    }

    fun getServerStorage(): ImmichServerStorage? {
        val url = immichServerContext.getServerUrl() ?: run {
            log.error("Could not get server url")
            return null
        }
        val apiKey = immichUserContext.getCurrentUserApiKey() ?: run {
            log.error("Could not get api key")
            return null
        }
        return immichClientFactory.create(url)
        .getServerStorage(apiKey)
    }

}