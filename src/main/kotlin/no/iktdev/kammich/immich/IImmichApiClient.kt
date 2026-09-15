package no.iktdev.kammich.immich

import no.iktdev.kammich.immich.models.AlbumResponseDto
import no.iktdev.kammich.immich.models.AssetResponseDto
import no.iktdev.kammich.models.internal.immich.UploadAssetRequest
import no.iktdev.kammich.models.shared.immich.api.*
import java.util.*


interface IImmichApiClient {
    fun uploadFile(upload: UploadAssetRequest): UUID?
    fun getMyApiKey(): ImmichApiKeyPostResponseDto?
    fun me(): ImmichUserMe
    fun getServerPing(): Boolean
    fun getServerSupportedMediaTypes(): ImmichSupportedMediaTypes?
    fun getServerVersion(): ImmichServerVersion?
    fun getServerFeatures(): ImmichServerFeatures?
    fun getServerConfig(): ImmichServerConfig?
    fun getServerStorage(): ImmichServerStorage?
    fun createAlbum(albumName: String, albumDescription: String?): UUID
    fun updateAlbum(albumId: UUID, albumName: String?, albumDescription: String?): AlbumResponseDto
    fun addPhotoToAlbum(albumId: UUID, assetIds: List<UUID>): Boolean
    fun getFileInfo(assetId: UUID): AssetResponseDto
}