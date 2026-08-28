package no.iktdev.kammich.models.shared

data class Version(
    val kammichVersion: String,
    val kammichGithubVersion: String,
    val updateAvailable: Boolean,
    val updatable: Boolean
)