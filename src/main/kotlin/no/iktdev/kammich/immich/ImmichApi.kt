package no.iktdev.kammich.immich

import no.iktdev.kammich.immich.models.AlbumResponseDto
import no.iktdev.kammich.immich.models.AssetResponseDto
import no.iktdev.kammich.immich.models.LoginResponseDto
import no.iktdev.kammich.models.internal.immich.UploadAssetRequest
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPost
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPostResponse
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPostResponseDto
import no.iktdev.kammich.models.shared.immich.api.ImmichAuthenticationLogin
import no.iktdev.kammich.models.shared.immich.api.ImmichAuthenticationLoginResponse
import no.iktdev.kammich.models.shared.immich.api.ImmichServerConfig
import no.iktdev.kammich.models.shared.immich.api.ImmichServerFeatures
import no.iktdev.kammich.models.shared.immich.api.ImmichServerStorage
import no.iktdev.kammich.models.shared.immich.api.ImmichServerVersion
import no.iktdev.kammich.models.shared.immich.api.ImmichSupportedMediaTypes
import no.iktdev.kammich.models.shared.immich.api.ImmichUserMe
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import java.util.UUID

interface ImmichApi {
    fun login(payload: ImmichAuthenticationLogin): LoginResponseDto
    fun logout(sessionToken: String)

    // Hent brukerinfo via aktiv sesjonstoken
    fun meBySession(sessionToken: String): ImmichUserMe

    // Hent brukerinfo via permanent API-nøkkel
    fun meByApiKey(apiKey: String): ImmichUserMe

    fun createApiKey(sessionToken: String, payload: ImmichApiKeyPost): ImmichApiKeyPostResponse
    fun getMyApiKey(apiKey: String): ImmichApiKeyPostResponseDto?

    fun getProfileImage(apiKey: String, userId: UUID): ByteArray?
    fun getServerSupportedMediaTypes(): ImmichSupportedMediaTypes?
    fun getServerVersion(): ImmichServerVersion?
    fun getServerPing(): Boolean
    fun getServerFeatures(): ImmichServerFeatures?
    fun getServerConfig(): ImmichServerConfig?
    fun getServerStorage(apiKey: String): ImmichServerStorage?
    fun uploadFile(apiKey: String, upload: UploadAssetRequest): UUID?
    fun createAlbum(apiKey: String, albumName: String, albumDescription: String?): UUID
    fun updateAlbum(apiKey: String, albumId: UUID, albumName: String?, albumDescription: String?): AlbumResponseDto
    fun addPhotoToAlbum(apiKey: String, albumId: UUID, assetIds: List<UUID>): Boolean
    fun getFileInfo(apiKey: String, assetId: UUID): AssetResponseDto
}