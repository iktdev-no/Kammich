package no.iktdev.kammich.models.shared.immich.api

data class ImmichServerVersion(val major: Int, val minor: Int, val patch: Int, val preRelease: Int?) {
}