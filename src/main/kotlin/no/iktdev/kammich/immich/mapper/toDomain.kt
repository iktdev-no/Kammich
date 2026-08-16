package no.iktdev.kammich.immich.mapper

import no.iktdev.kammich.immich.models.ApiKeyCreateResponseDto
import no.iktdev.kammich.immich.models.ApiKeyResponseDto
import no.iktdev.kammich.immich.models.ServerConfigDto
import no.iktdev.kammich.immich.models.ServerFeaturesDto
import no.iktdev.kammich.immich.models.ServerMediaTypesResponseDto
import no.iktdev.kammich.immich.models.ServerStorageResponseDto
import no.iktdev.kammich.immich.models.ServerVersionResponseDto
import no.iktdev.kammich.immich.models.UserAdminResponseDto
import no.iktdev.kammich.immich.models.UserAvatarColor as GeneratedAvatarColor
import no.iktdev.kammich.immich.models.UserStatus as GeneratedUserStatus
import no.iktdev.kammich.models.shared.immich.api.*

fun UserAdminResponseDto.toDomain(): ImmichUserMe {
    return ImmichUserMe(
        avatarColor = when (this.avatarColor) {
            GeneratedAvatarColor.PRIMARY -> UserAvatarColor.primary
            GeneratedAvatarColor.PINK -> UserAvatarColor.pink
            GeneratedAvatarColor.RED -> UserAvatarColor.red
            GeneratedAvatarColor.YELLOW -> UserAvatarColor.yellow
            GeneratedAvatarColor.BLUE -> UserAvatarColor.blue
            GeneratedAvatarColor.GREEN -> UserAvatarColor.green
            GeneratedAvatarColor.PURPLE -> UserAvatarColor.purple
            GeneratedAvatarColor.ORANGE -> UserAvatarColor.orange
            GeneratedAvatarColor.GRAY -> UserAvatarColor.gray
            GeneratedAvatarColor.AMBER -> UserAvatarColor.amber
            else -> UserAvatarColor.primary // Fallback
        },
        createdAt = this.createdAt.toInstant(),
        deletedAt = this.deletedAt?.toInstant(),
        email = this.email,
        id = this.id,
        isAdmin = this.isAdmin,
        license = this.license?.let {
            ImmichUserLicense(
                activatedAt = it.activatedAt.toInstant(),
                activationKey = it.activationKey,
                licenseKey = it.licenseKey
            )
        },
        name = this.name,
        oauthId = this.oauthId,
        profileChangedAt = this.profileChangedAt.toInstant(),
        profileImagePath = this.profileImagePath,
        // Konverterer Int? til Long? hvis din modell bruker Long
        quotaSizeInBytes = this.quotaSizeInBytes?.toLong(),
        quotaUsageInBytes = this.quotaUsageInBytes?.toLong(),
        shouldChangePassword = this.shouldChangePassword,
        status = when (this.status) {
            GeneratedUserStatus.ACTIVE -> ImmichUserStatus.active
            GeneratedUserStatus.REMOVING -> ImmichUserStatus.removing
            GeneratedUserStatus.DELETED -> ImmichUserStatus.deleted
            else -> ImmichUserStatus.active
        },
        storageLabel = this.storageLabel,
        updatedAt = this.updatedAt.toInstant()
    )
}

fun ApiKeyCreateResponseDto.toDomain(): ImmichApiKeyPostResponse {
    return ImmichApiKeyPostResponse(
        secret = this.secret,
        apiKey = apiKey.toDomain(),
    )
}

fun ApiKeyResponseDto.toDomain(): ImmichApiKeyPostResponseDto {
    return ImmichApiKeyPostResponseDto(
        createdAt = this.createdAt.toInstant(),
        updatedAt = this.updatedAt.toInstant(),
        permissions = this.permissions.map { it.name },
        name = this.name,
        id = this.id,
    )
}

fun ServerMediaTypesResponseDto.toDomain(): ImmichSupportedMediaTypes {
    return ImmichSupportedMediaTypes(
        images = this.image,
        sidecar = this.sidecar,
        videos = this.video,
    )
}

fun ServerFeaturesDto.toDomain(): ImmichServerFeatures {
    return ImmichServerFeatures(
        configFileAvailable = configFile,
        duplicateDetectionEnabled = duplicateDetection,
        emailNotificationEnabled = email,
        facialRecognitionEnabled = facialRecognition,
        importFacesEnabled = importFaces,
        mapEnabled = map,
        oauthEnabled = oauth,
        oauthAutoLaunchEnabled = oauthAutoLaunch,
        ocrEnabled = ocr,
        passwordLoginEnabled = passwordLogin,
        realtimeTranscodingEnabled = realtimeTranscoding,
        reverseGeocodingEnabled = reverseGeocoding,
        searchEnabled = search,
        sidecarSupported = sidecar,
        smartSearchEnabled = smartSearch,
        trashEnabled = trash
    )
}

fun ServerConfigDto.toDomain(): ImmichServerConfig {
    return ImmichServerConfig(
        externalDomain = externalDomain,
        isInitialized = isInitialized,
        isOnboarded = isOnboarded,
        loginPageMessage = loginPageMessage,
        maintenanceMode = maintenanceMode,
        mapDarkStyleUrl = mapDarkStyleUrl,
        mapLightStyleUrl = mapLightStyleUrl,
        minFaces = minFaces,
        oauthButtonText = oauthButtonText,
        publicUsersEnabled = publicUsers,
        trashDays = trashDays,
        userDeleteDelay = userDeleteDelay
    )
}

fun ServerStorageResponseDto.toDomain(): ImmichServerStorage {
    return ImmichServerStorage(
        diskAvailable = diskAvailable,
        diskAvailableRaw = diskAvailableRaw,
        diskSize = diskSize,
        diskSizeRaw = diskSizeRaw,
        diskUsagePercentage = diskUsagePercentage,
        diskUse = diskUse,
        diskUseRaw = diskUseRaw
    )
}

fun ServerVersionResponseDto.toDomain(): ImmichServerVersion {
    return ImmichServerVersion(
            major = this.major.toInt(),
        minor = this.minor.toInt(),
        patch = this.patch.toInt(),
        preRelease = this.prerelease?.toInt()
    )
}