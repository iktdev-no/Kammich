package no.iktdev.kammich.immich

import no.iktdev.kammich.database.tables.ImmichAuthenticationTable
import no.iktdev.kammich.database.tables.ImmichAuthenticationTable.serverUrl
import no.iktdev.kammich.database.tables.ImmichAuthenticationTable.toPersistedApiKey
import no.iktdev.kammich.database.tables.ImmichUsersTable
import no.iktdev.kammich.database.tables.ImmichUsersTable.toPersistedImmichUser
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.models.shared.immich.ImmichLoginRequest
import no.iktdev.kammich.models.shared.immich.ImmichServerAccess
import no.iktdev.kammich.models.shared.immich.ImmichUserAccesses
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPost
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPostResponse
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPostResponseDto
import no.iktdev.kammich.models.shared.immich.api.ImmichUserMe
import no.iktdev.kammich.models.shared.immich.api.defaultPermissions
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.SSEImmichApiKeyInUse
import no.iktdev.kammich.sse.events.SSEImmichUser
import no.iktdev.kammich.util.gson
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.UUID
import javax.management.Query.and

@Service
class ImmichService(
    private val immichClientFactory: ImmichClientFactory,
    private val immichVerificationService: ImmichVerificationService,
    private val sseManager: SseManager
) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    fun authenticateAndCreateApiKey(payload: ImmichLoginRequest): ImmichUserMe {
        val trimmedInput = payload.address.trim().removeSuffix("/")

        // Bestem URL-er å teste
        val urlsToTry = if (!trimmedInput.startsWith("http://", ignoreCase = true) &&
            !trimmedInput.startsWith("https://", ignoreCase = true)) {
            listOf("https://$trimmedInput", "http://$trimmedInput")
        } else {
            listOf(trimmedInput)
        }

        var lastException: Throwable? = null

        for ((index, url) in urlsToTry.withIndex()) {
            try {
                val immichClient = immichClientFactory.create(url)

                // Steg 1: Logg inn og få sesjonstoken + brukerinfo direkte
                val loginResult = immichClient.login(payload.toImmichUserLogin())

                // Steg 2: Generer permanent API-nøkkel med nødvendige rettigheter
                val apiKeyPayload = ImmichApiKeyPost(
                    name = "Kammich Ingest Service",
                    permissions = defaultPermissions
                )

                val apiKeyResponse = immichClient.createApiKey(loginResult.accessToken, apiKeyPayload)

                log.info("Got API key successfully for user: ${loginResult.userEmail} using URL: $url")

                // Steg 3: Lagre i databasen
                storeUserAndApiKey(loginResult.userId, url, apiKeyResponse, immichClient)

                val me = immichClient.meByApiKey(apiKeyResponse.secret)
                sseManager.send(SSEImmichUser(me))
                sseManager.send(SSEImmichApiKeyInUse(apiKeyResponse.apiKey))
                return me

            } catch (e: Exception) {
                lastException = e
                // Hvis vi har flere URL-er igjen å prøve (f.eks. feilet https), logg og fortsett til http
                if (index < urlsToTry.size - 1) {
                    log.warn("Failed to connect using $url, trying fallback... Error: ${e.message}")
                }
            }
        }

        // Hvis alt feiler, kaster vi den siste feilen videre
        throw lastException ?: RuntimeException("Failed to authenticate with any URL variant")
    }

    private fun storeUserAndApiKey(userId: UUID, serverUrl: String, payload: ImmichApiKeyPostResponse, immichClient: ImmichApi) {
        withTransaction {
            val userIdStr = userId.toString()

            // 1. Hent "me" info for å lagre brukeren korrekt
            val me = immichClient.meByApiKey(payload.secret)

            // 2. Sett alle andre brukere til inaktive, og opprett eller oppdater aktuell bruker
            ImmichUsersTable.update({ ImmichUsersTable.isActive eq true }) {
                it[isActive] = false
            }

            val existingUser = ImmichUsersTable.selectAll()
                .where { ImmichUsersTable.userId eq userIdStr }
                .singleOrNull()

            if (existingUser == null) {
                ImmichUsersTable.insert {
                    it[ImmichUsersTable.userId] = userIdStr
                    it[name] = me.name
                    it[email] = me.email
                    it[createdAt] = me.createdAt.toString()
                    it[isActive] = true
                    it[data] = gson.toJson(me)
                }
            } else {
                ImmichUsersTable.update({ ImmichUsersTable.userId eq userIdStr }) {
                    it[name] = me.name
                    it[email] = me.email
                    it[isActive] = true
                    it[data] = gson.toJson(me)
                }
            }

            // 3. Deaktiver gamle servere for denne brukeren om ønskelig, eller la dem ligge.
            // Her deaktiverer vi andre aktive servere for denne brukeren og setter den nye til aktiv:
            ImmichAuthenticationTable.update({ ImmichAuthenticationTable.userId eq userIdStr }) {
                it[isActive] = false
            }

            ImmichAuthenticationTable.insert {
                it[ImmichAuthenticationTable.userId] = userIdStr
                it[ImmichAuthenticationTable.apiKeyId] = payload.apiKey.id.toString()
                it[ImmichAuthenticationTable.serverUrl] = serverUrl
                it[apiKey] = payload.secret
                it[createdAt] = payload.apiKey.createdAt.toString()
                it[isActive] = true
                it[data] = gson.toJson(payload.apiKey)
            }
        }
    }

    fun getUsersWithAccesses(): List<ImmichUserAccesses> {
        return withTransaction {
            ImmichUsersTable.selectAll().map { it.toPersistedImmichUser() }
                .map { p ->
                    val userMe = gson.fromJson(p.data, ImmichUserMe::class.java)

                    val servers = ImmichAuthenticationTable.selectAll()
                        .where { ImmichAuthenticationTable.userId eq p.userId }
                        .map { it.toPersistedApiKey() }
                        .map { auth ->
                            val keyDets = gson.fromJson(auth.data, ImmichApiKeyPostResponseDto::class.java)
                            ImmichServerAccess(
                                keyName = keyDets.name,
                                keyId = keyDets.id.toString(),
                                serverUrl = auth.serverUrl,
                                isActive = auth.isActive,
                                createdAt = auth.createdAt,
                            )
                        }

                    ImmichUserAccesses(
                        user = userMe,
                        isActive = p.isActive,
                        servers = servers
                    )
                }
        }.getOrDefault(emptyList())
    }

    fun deleteApiKey(apiKeyId: String): ResponseEntity<Void> {
        return withTransaction {
            val deletedRows = ImmichAuthenticationTable.deleteWhere {
                ImmichAuthenticationTable.apiKeyId eq apiKeyId
            }

            when {
                deletedRows == 1 -> {
                    ResponseEntity.noContent().build<Void>()
                }
                deletedRows > 1 -> {
                    // Kaster en feil slik at transaksjonen rulles tilbake (rollback)
                    error("Slettet $deletedRows rader for apiKeyId=$apiKeyId, forventet nøyaktig 1.")
                }
                else -> {
                    ResponseEntity.notFound().build<Void>()
                }
            }
        }.getOrDefault(ResponseEntity.internalServerError().build())
    }

}