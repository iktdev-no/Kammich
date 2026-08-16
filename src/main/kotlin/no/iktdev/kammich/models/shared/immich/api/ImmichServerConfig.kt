package no.iktdev.kammich.models.shared.immich.api

data class ImmichServerConfig(
    val externalDomain: String,
    val isInitialized: Boolean,
    val isOnboarded: Boolean,
    val loginPageMessage: String,
    val maintenanceMode: Boolean,
    val mapDarkStyleUrl: String,
    val mapLightStyleUrl: String,
    val minFaces: Long,
    val oauthButtonText: String,
    val publicUsersEnabled: Boolean,
    val trashDays: Long,
    val userDeleteDelay: Long
)