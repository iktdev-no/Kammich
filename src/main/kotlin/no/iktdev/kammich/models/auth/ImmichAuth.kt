package no.iktdev.kammich.models.auth

sealed interface ImmichAuth {
    data class ApiKey(val key: String) : ImmichAuth
    data class OAuth(val accessToken: String, val refreshToken: String) : ImmichAuth
}
