package no.iktdev.kammich.immich.client

import no.iktdev.kammich.immich.api.AssetsApi
import no.iktdev.kammich.immich.models.*
import okhttp3.Call
import org.openapitools.client.infrastructure.*
import java.io.File
import java.io.IOException
import java.time.OffsetDateTime
import java.util.UUID


class ExtensiveImmichAssetUploadClient(basePath: kotlin.String = defaultBasePath, client: Call.Factory = ApiClient.defaultClient): AssetsApi(basePath, client) {
    @Suppress("UNCHECKED_CAST")
    private fun uploadAssetRequestConfigClean(
        assetData: File,
        fileCreatedAt: OffsetDateTime,
        fileModifiedAt: OffsetDateTime,
        key: String?,
        slug: String?,
        xImmichChecksum: String?,
        duration: Long?,
        filename: String?,
        isFavorite: Boolean?,
        livePhotoVideoId: UUID?,
        metadata: List<AssetMetadataUpsertItemDto>?,
        sidecarData: File?,
        visibility: AssetVisibility?
    ): RequestConfig<Map<String, PartConfig<*>>> {

        // Bygg bodyen og filtrer bort alt som er null (unngår Zod-valideringsfeil på serveren)
        val localVariableBody = listOfNotNull(
            ("assetData" to PartConfig(body = assetData, headers = mutableMapOf())) as Pair<String, PartConfig<*>>,
            duration?.let { ("duration" to PartConfig(body = it, headers = mutableMapOf())) as Pair<String, PartConfig<*>> },
            ("fileCreatedAt" to PartConfig(body = fileCreatedAt, headers = mutableMapOf())) as Pair<String, PartConfig<*>>,
            ("fileModifiedAt" to PartConfig(body = fileModifiedAt, headers = mutableMapOf())) as Pair<String, PartConfig<*>>,
            filename?.let { ("filename" to PartConfig(body = it, headers = mutableMapOf())) as Pair<String, PartConfig<*>> },
            isFavorite?.let { ("isFavorite" to PartConfig(body = it, headers = mutableMapOf())) as Pair<String, PartConfig<*>> },
            livePhotoVideoId?.let { ("livePhotoVideoId" to PartConfig(body = it, headers = mutableMapOf())) as Pair<String, PartConfig<*>> },
            metadata?.let { ("metadata" to PartConfig(body = it, headers = mutableMapOf())) as Pair<String, PartConfig<*>> },
            sidecarData?.let { ("sidecarData" to PartConfig(body = it, headers = mutableMapOf())) as Pair<String, PartConfig<*>> },
            visibility?.let { ("visibility" to PartConfig(body = it, headers = mutableMapOf())) as Pair<String, PartConfig<*>> }
        ).toMap()

        val localVariableQuery: MultiValueMap = mutableMapOf<String, List<String>>().apply {
            if (key != null) {
                put("key", listOf(key.toString()))
            }
            if (slug != null) {
                put("slug", listOf(slug.toString()))
            }
        }

        val localVariableHeaders: MutableMap<String, String> = mutableMapOf("Content-Type" to "multipart/form-data")
        xImmichChecksum?.apply { localVariableHeaders["x-immich-checksum"] = this.toString() }
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.POST,
            path = "/assets",
            query = localVariableQuery,
            headers = localVariableHeaders,
            requiresAuthentication = true,
            body = localVariableBody
        )
    }

    @Throws(IllegalStateException::class, IOException::class)
    private fun uploadAssetWithHttpInfoSlim(assetData: java.io.File, fileCreatedAt: java.time.OffsetDateTime, fileModifiedAt: java.time.OffsetDateTime, key: kotlin.String?, slug: kotlin.String?, xImmichChecksum: kotlin.String?, duration: kotlin.Long?, filename: kotlin.String?, isFavorite: kotlin.Boolean?, livePhotoVideoId: java.util.UUID?, metadata: kotlin.collections.List<AssetMetadataUpsertItemDto>?, sidecarData: java.io.File?, visibility: AssetVisibility?) : ApiResponse<AssetMediaResponseDto?> {
        val localVariableConfig = uploadAssetRequestConfigClean(assetData = assetData, fileCreatedAt = fileCreatedAt, fileModifiedAt = fileModifiedAt, key = key, slug = slug, xImmichChecksum = xImmichChecksum, duration = duration, filename = filename, isFavorite = isFavorite, livePhotoVideoId = livePhotoVideoId, metadata = metadata, sidecarData = sidecarData, visibility = visibility)

        return request<Map<String, PartConfig<*>>, AssetMediaResponseDto>(
            localVariableConfig
        )
    }

    fun uploadAssetSlim(assetData: java.io.File, fileCreatedAt: java.time.OffsetDateTime, fileModifiedAt: java.time.OffsetDateTime, key: kotlin.String? = null, slug: kotlin.String? = null, xImmichChecksum: kotlin.String? = null, duration: kotlin.Long? = null, filename: kotlin.String? = null, isFavorite: kotlin.Boolean? = null, livePhotoVideoId: java.util.UUID? = null, metadata: kotlin.collections.List<AssetMetadataUpsertItemDto>? = null, sidecarData: java.io.File? = null, visibility: AssetVisibility? = null) : AssetMediaResponseDto {
        val localVarResponse = uploadAssetWithHttpInfoSlim(assetData = assetData, fileCreatedAt = fileCreatedAt, fileModifiedAt = fileModifiedAt, key = key, slug = slug, xImmichChecksum = xImmichChecksum, duration = duration, filename = filename, isFavorite = isFavorite, livePhotoVideoId = livePhotoVideoId, metadata = metadata, sidecarData = sidecarData, visibility = visibility)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as AssetMediaResponseDto
            ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
            ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
            ResponseType.ClientError -> {
                val localVarError = localVarResponse as ClientError<*>
                throw ClientException("Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}", localVarError.statusCode, localVarResponse)
            }
            ResponseType.ServerError -> {
                val localVarError = localVarResponse as ServerError<*>
                throw ServerException("Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()} ${localVarError.body}", localVarError.statusCode, localVarResponse)
            }
        }
    }
}

