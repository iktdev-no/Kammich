package no.iktdev.kammich.models.shared.immich.api

data class ImmichServerFeatures(
    val configFileAvailable: Boolean,
    val duplicateDetectionEnabled: Boolean,
    val emailNotificationEnabled: Boolean,
    val facialRecognitionEnabled: Boolean,
    val importFacesEnabled: Boolean,
    val mapEnabled: Boolean,
    val oauthEnabled: Boolean,
    val oauthAutoLaunchEnabled: Boolean,
    val ocrEnabled: Boolean,
    val passwordLoginEnabled: Boolean,
    val realtimeTranscodingEnabled: Boolean,
    val reverseGeocodingEnabled: Boolean,
    val searchEnabled: Boolean,
    val sidecarSupported: Boolean,
    val smartSearchEnabled: Boolean,
    val trashEnabled: Boolean
)