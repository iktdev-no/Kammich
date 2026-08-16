package no.iktdev.kammich.immich.client

import no.iktdev.kammich.immich.ImmichApi
import no.iktdev.kammich.immich.api.APIKeysApi // Eller ApiKeysApi / KeyApi
import no.iktdev.kammich.immich.api.AuthenticationApi
import no.iktdev.kammich.immich.api.ServerApi
import no.iktdev.kammich.immich.api.UsersApi
import no.iktdev.kammich.immich.exceptions.ImmichConnectionUnavailableException
import no.iktdev.kammich.immich.exceptions.ImmichLoginIncorrectUsernameOrPasswordException
import no.iktdev.kammich.immich.mapper.fromDomain
import no.iktdev.kammich.immich.mapper.toDomain
import no.iktdev.kammich.immich.models.LoginCredentialDto
import no.iktdev.kammich.immich.models.LoginResponseDto
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPost
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPostResponse
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPostResponseDto
import no.iktdev.kammich.models.shared.immich.api.ImmichAuthenticationLogin
import no.iktdev.kammich.models.shared.immich.api.ImmichServerConfig
import no.iktdev.kammich.models.shared.immich.api.ImmichServerFeatures
import no.iktdev.kammich.models.shared.immich.api.ImmichServerStorage
import no.iktdev.kammich.models.shared.immich.api.ImmichServerVersion
import no.iktdev.kammich.models.shared.immich.api.ImmichSupportedMediaTypes
import no.iktdev.kammich.models.shared.immich.api.ImmichUserMe
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.openapitools.client.infrastructure.ApiClient
import org.openapitools.client.infrastructure.ClientError
import org.openapitools.client.infrastructure.ClientException
import org.openapitools.client.infrastructure.ResponseType
import org.openapitools.client.infrastructure.ServerError
import org.openapitools.client.infrastructure.ServerException
import org.openapitools.client.infrastructure.Success
import org.slf4j.LoggerFactory
import java.io.File
import java.net.ConnectException
import java.util.UUID

class ImmichClient(
    private val serverUrl: String
) : ImmichApi {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun createClient(headerName: String, headerValue: String): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val authenticatedRequest = chain.request().newBuilder()
                .header(headerName, headerValue)
                .build()
            chain.proceed(authenticatedRequest)
        }

        // Bygg videre på ApiClient sin egen defaultClient (som er en OkHttpClient)
        return (ApiClient.defaultClient as OkHttpClient).newBuilder()
            .addInterceptor(authInterceptor)
            .build()
    }

    private fun sessionClient(token: String) = createClient("x-immich-user-token", token)
    private fun apiKeyClient(key: String) = createClient("x-api-key", key)

    // Uten auth
    private val authApi = AuthenticationApi(serverUrl)

    override fun login(payload: ImmichAuthenticationLogin): LoginResponseDto {
        log.info("URL: ${authApi.baseUrl}")
        val loginCredentialDto = LoginCredentialDto(payload.email, payload.password)

        val localVarResponse = try {
            authApi.loginWithHttpInfo(loginCredentialDto = loginCredentialDto)
        } catch (e: ConnectException) {
            throw ImmichConnectionUnavailableException("Could not connect to the server")
        }

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as LoginResponseDto
            ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
            ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
            ResponseType.ClientError -> {
                val localVarError = localVarResponse as ClientError<*>
                if (localVarError.statusCode == 401) {
                    throw ImmichLoginIncorrectUsernameOrPasswordException("Feil brukernavn eller passord.")
                }
                throw ClientException(
                    "Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}",
                    localVarError.statusCode,
                    localVarResponse
                )
            }
            ResponseType.ServerError -> {
                val localVarError = localVarResponse as ServerError<*>
                throw ServerException(
                    "Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()} ${localVarError.body}",
                    localVarError.statusCode,
                    localVarResponse
                )
            }
        }
    }

    override fun logout(sessionToken: String) {
        AuthenticationApi(serverUrl, sessionClient(sessionToken)).logout()
    }

    override fun meBySession(sessionToken: String): ImmichUserMe {
        return UsersApi(serverUrl, sessionClient(sessionToken)).getMyUser().toDomain()
    }

    override fun meByApiKey(apiKey: String): ImmichUserMe {
        return UsersApi(serverUrl, apiKeyClient(apiKey)).getMyUser().toDomain()
    }

    override fun createApiKey(sessionToken: String, payload: ImmichApiKeyPost): ImmichApiKeyPostResponse {
        return APIKeysApi(serverUrl, sessionClient(sessionToken)).createApiKey(payload.fromDomain()).toDomain()
    }

    override fun getMyApiKey(apiKey: String): ImmichApiKeyPostResponseDto? {
        return try {
            APIKeysApi(serverUrl, apiKeyClient(apiKey)).getMyApiKey().toDomain()
        } catch (e: Exception) {
            // Sjekk om det er en 404 basert på feilmelding, statuskode eller unntakstype
            when {
                // Eksempelvis hvis klienten har en statusCode eller message som inneholder 404:
                e.message?.contains("404") == true -> {
                    log.warn("API-nøkkelen ble ikke funnet på serveren (404). Den har sannsynligvis blitt sletta.")
                    null
                }
                // Hvis du bruker f.eks. Spring sin RestClient / RestTemplate:
                // e is HttpClientErrorException.NotFound -> null

                // Hvis det er en annen feil, kaster vi den videre så den ikke skjules
                else -> throw e
            }
        }
    }

    override fun getProfileImage(apiKey: String, userId: UUID): ByteArray? {
        var tempFile: File? = null
        return try {
            tempFile = UsersApi(serverUrl, apiKeyClient(apiKey)).getProfileImage(userId)

            if (tempFile.exists() && tempFile.length() > 0) {
                tempFile.readBytes()
            } else {
                null
            }
        } catch (e: Exception) {
            // Hvis brukeren ikke har et profilbilde, svarer Immich ofte med 404.
            // Vi fanger den opp og returnerer null i stedet for å kaste unntak.
            if (e.message?.contains("404") == true) {
                null
            } else {
                log.warn("Could not fetch profile image for user $userId: ${e.message}")
                null
            }
        } finally {
            // Alltid slett temp-filen generert av klienten, uansett om det gikk bra eller feilet
            try {
                tempFile?.let { if (it.exists()) it.delete() }
            } catch (e: Exception) {
                log.debug("Failed to delete temp profile image file: ${e.message}")
            }
        }
    }

    override fun getServerSupportedMediaTypes(): ImmichSupportedMediaTypes? {
        return try {
            ServerApi(serverUrl)
                .getSupportedMediaTypes().toDomain()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun getServerVersion(): ImmichServerVersion? {
        return try {
            ServerApi(serverUrl)
                .getServerVersion().toDomain()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun getServerPing(): Boolean {
        return try {
            ServerApi(serverUrl).pingServer()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun getServerFeatures(): ImmichServerFeatures? {
        return try {
            ServerApi(serverUrl).getServerFeatures().toDomain()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun getServerConfig(): ImmichServerConfig? {
        return try {
            ServerApi(serverUrl).getServerConfig().toDomain()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun getServerStorage(apiKey: String): ImmichServerStorage? {
        return try {
            val response = ServerApi(serverUrl, apiKeyClient(apiKey))
                .getStorage()
            log.info(response.toString())
            response.toDomain()
        } catch (e: Exception) {
         e.printStackTrace()
         null
        }
    }


}