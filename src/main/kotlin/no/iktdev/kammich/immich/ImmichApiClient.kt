package no.iktdev.kammich.immich

import no.iktdev.kammich.immich.models.AlbumResponseDto
import no.iktdev.kammich.immich.models.AssetResponseDto
import no.iktdev.kammich.models.internal.immich.UploadAssetRequest
import no.iktdev.kammich.models.shared.immich.api.*
import java.util.*

class ImmichApiClient(
    private val client: ImmichApi,
    private val apiKey: String
) : IImmichApiClient {

    override fun uploadFile(upload: UploadAssetRequest): UUID? =
        client.uploadFile(apiKey, upload)

    override fun getMyApiKey(): ImmichApiKeyPostResponseDto? =
        client.getMyApiKey(apiKey)

    override fun me(): ImmichUserMe =
        client.meByApiKey(apiKey)

    override fun getServerPing(): Boolean =
        client.getServerPing()

    override fun getServerSupportedMediaTypes(): ImmichSupportedMediaTypes? =
        client.getServerSupportedMediaTypes()

    override fun getServerVersion(): ImmichServerVersion? =
        client.getServerVersion()

    override fun getServerFeatures(): ImmichServerFeatures? =
        client.getServerFeatures()

    override fun getServerConfig(): ImmichServerConfig? =
        client.getServerConfig()

    override fun getServerStorage(): ImmichServerStorage? =
        client.getServerStorage(apiKey)

    override fun createAlbum(albumName: String, albumDescription: String?): UUID =
        client.createAlbum(apiKey, albumName, albumDescription)

    override fun updateAlbum(albumId: UUID, albumName: String?, albumDescription: String?): AlbumResponseDto =
        client.updateAlbum(apiKey, albumId, albumName, albumDescription)

    override fun addPhotoToAlbum(albumId: UUID, assetIds: List<UUID>): Boolean =
        client.addPhotoToAlbum(apiKey, albumId, assetIds)

    override fun getFileInfo(assetId: UUID): AssetResponseDto =
        client.getFileInfo(apiKey, assetId)
}