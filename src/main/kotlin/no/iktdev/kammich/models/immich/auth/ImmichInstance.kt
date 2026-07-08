package no.iktdev.kammich.models.immich.auth

data class ImmichInstance(
    val url: String,            // URL til Immich-serveren
    val auth: ImmichAuth        // Nøkkel eller OAuth
)