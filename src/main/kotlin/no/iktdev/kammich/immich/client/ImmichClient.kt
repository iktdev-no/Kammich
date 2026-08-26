package no.iktdev.kammich.immich.client

import com.google.gson.Gson
import kotlinx.datetime.toLocalDateTime
import no.iktdev.kammich.asOffsetDateTime
import no.iktdev.kammich.immich.ImmichApi
import no.iktdev.kammich.immich.api.APIKeysApi // Eller ApiKeysApi / KeyApi
import no.iktdev.kammich.immich.api.AlbumsApi
import no.iktdev.kammich.immich.api.AssetsApi
import no.iktdev.kammich.immich.api.AuthenticationApi
import no.iktdev.kammich.immich.api.ServerApi
import no.iktdev.kammich.immich.api.UsersApi
import no.iktdev.kammich.immich.exceptions.ImmichConnectionUnavailableException
import no.iktdev.kammich.immich.exceptions.ImmichException
import no.iktdev.kammich.immich.exceptions.ImmichLoginIncorrectUsernameOrPasswordException
import no.iktdev.kammich.immich.mapper.fromDomain
import no.iktdev.kammich.immich.mapper.toDomain
import no.iktdev.kammich.immich.models.AlbumResponseDto
import no.iktdev.kammich.immich.models.AlbumsAddAssetsDto
import no.iktdev.kammich.immich.models.AssetResponseDto
import no.iktdev.kammich.immich.models.BulkIdsDto
import no.iktdev.kammich.immich.models.CreateAlbumDto
import no.iktdev.kammich.immich.models.LoginCredentialDto
import no.iktdev.kammich.immich.models.LoginResponseDto
import no.iktdev.kammich.immich.models.UpdateAlbumDto
import no.iktdev.kammich.models.internal.immich.UploadAssetRequest
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
import org.openapitools.client.infrastructure.ApiResponse
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
        return ApiClient.defaultClient.newBuilder()
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


    override fun uploadFile(apiKey: String, upload: UploadAssetRequest): UUID {
        val client = ExtensiveImmichAssetUploadClient(serverUrl, apiKeyClient(apiKey))
        return try {
            log.info("""
                Upload file exists: ${upload.file.exists()}
                Filename: ${upload.file.name}
                Created: ${upload.createdAt.asOffsetDateTime()}
                Modified at: ${upload.modifiedAt.asOffsetDateTime()}
            """.trimIndent())
            val out = client.uploadAssetSlim(
                assetData = upload.file,
                fileCreatedAt = upload.createdAt.asOffsetDateTime(),
                fileModifiedAt = upload.modifiedAt.asOffsetDateTime(),
                filename = upload.file.name
            )

            log.info(Gson().toJson(out))
            out.id
        } catch (e: ClientException) {
            // Dette henter ut den faktiske JSON-feilen fra Immich-serveren
            val errorBody = Gson().toJson(e.response)
            log.error("Immich 400 Bad Request Detaljer: statusCode=${e.statusCode}, body=$errorBody", e)
            throw e
        } catch (e: Exception) {
            log.error("Feilet ved opplasting: ${e.message}", e)
            throw e
        }
    }

    override fun createAlbum(apiKey: String, albumName: String, albumDescription: String?): UUID {
        val client = AlbumsApi(serverUrl, apiKeyClient(apiKey))
        return try {
            val out = client.createAlbum(CreateAlbumDto(albumName = albumName, description = albumDescription))
            out.id
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override fun updateAlbum(apiKey: String, albumId: UUID, albumName: String?, albumDescription: String?): AlbumResponseDto {
        val client = AlbumsApi(serverUrl, apiKeyClient(apiKey))
        val updatePayload = UpdateAlbumDto(albumName = albumName, description = albumDescription)
        log.info("Updating album: $albumId with payload: ${Gson().toJson(updatePayload)}")
        return client.updateAlbumInfo(albumId, updatePayload)
    }

    override fun addPhotoToAlbum(apiKey: String, albumId: UUID, assetIds: List<UUID>): Boolean {
        val client = AlbumsApi(serverUrl, apiKeyClient(apiKey))
        return tryImmich {
            client.addAssetsToAlbums(AlbumsAddAssetsDto(listOf(albumId), assetIds)).success
        }
    }

    override fun getFileInfo(apiKey: String, assetId: UUID): AssetResponseDto {
        val client = AssetsApi(serverUrl, apiKeyClient(apiKey))
        return tryImmich { client.getAssetInfo(assetId) }
    }


    // En ren wrapper for å holde på responsen uansett utfall
    sealed class ApiResult<out T> {
        data class Success<T>(val data: T, val statusCode: Int) : ApiResult<T>()
        data class Error(val message: String, val statusCode: Int, val rawResponse: Any) : ApiResult<Nothing>()
    }

    inline fun <T> tryImmich(block: () -> T): T {
        return try {
            block()
        } catch (e: ClientException) {
            e.statusCode
            print(e.response)
            throw e
        } catch (e: ServerException) {
            print(e.response)
            throw e
        } catch (e: ImmichException) {
            throw e
        } catch (e: Exception) {
            throw ImmichException("Uventet feil mot Immich: ${e.message}")
        }
    }


}