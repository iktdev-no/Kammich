package no.iktdev.kammich.models.shared.immich.api

import java.time.Instant
import java.util.UUID

data class ImmichApiKeysMe(
    val createdAt: Instant,
    val id: UUID,
    val name: String,
    val permissions: List<String>,
    val updatedAt: Instant
)

data class ImmichApiKeyPost(
    val name: String,
    val permissions: List<String>,
)

val defaultPermissions = listOf(
    "asset.read",
    "asset.update",
    "asset.upload",
    "asset.download",
    "album.create",
    "album.read",
    "album.update",
    "albumAsset.create",
    "albumAsset.delete",
    "user.read",
    "userProfileImage.read",
    "server.storage"
)

data class ImmichApiKeyPostResponse(
    val secret: String,
    val apiKey: ImmichApiKeyPostResponseDto
)

data class ImmichApiKeyPostResponseDto(
    val createdAt: Instant,
    val id: UUID,
    val name: String,
    val permissions: List<String>,
    val updatedAt: Instant,
)