package no.iktdev.kammich.models.shared.immich.api

import java.util.UUID

data class ImmichAuthenticationLogin(val email: String, val password: String)
data class ImmichAuthenticationLoginResponse(
    val accessToken: String,
    val isAdmin: Boolean,
    val isOnboarded: Boolean,
    val name: String,
    val profileImagePath: String,
    val shouldChangePassword: Boolean,
    val userEmail: String,
    val userId: UUID
)

