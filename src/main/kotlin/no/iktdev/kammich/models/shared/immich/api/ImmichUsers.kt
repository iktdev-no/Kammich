package no.iktdev.kammich.models.shared.immich.api

import java.util.UUID
import java.time.Instant

data class ImmichUserMe(
    val avatarColor: UserAvatarColor,
    val createdAt: Instant,
    val deletedAt: Instant? = null,
    val email: String,
    val id: UUID,
    val isAdmin: Boolean,
    val license: ImmichUserLicense? = null,
    val name: String,
    val oauthId: String? = null,
    val profileChangedAt: Instant? = null,
    val profileImagePath: String? = null,
    val quotaSizeInBytes: Long? = null,
    val quotaUsageInBytes: Long? = null,
    val shouldChangePassword: Boolean = false,
    val status: ImmichUserStatus? = ImmichUserStatus.active,
    val storageLabel: String? = null,
    val updatedAt: Instant? = null,
) {
}

data class ImmichUserLicense(val activatedAt: Instant, val activationKey: String, val licenseKey: String)

enum class UserAvatarColor {
    primary,
    pink,
    red,
    yellow,
    blue,
    green,
    purple,
    orange,
    gray,
    amber,
}

enum class ImmichUserStatus {
    active,
    removing,
    deleted,
}