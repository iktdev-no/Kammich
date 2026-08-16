package no.iktdev.kammich.models.shared.immich.api

data class ImmichSupportedMediaTypes(
    val images: List<String>,
    val sidecar: List<String>,
    val videos: List<String>,
) {
}