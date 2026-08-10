package no.iktdev.kammich.models.shared.immich

import no.iktdev.kammich.models.shared.immich.api.ImmichAuthenticationLogin

data class ImmichLoginRequest(
    val address: String,
    val email: String,
    val password: String,
) {
    fun toImmichUserLogin() = ImmichAuthenticationLogin(email, password)
}