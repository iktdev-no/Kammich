package no.iktdev.kammich.models.shared.tailscale

data class TailscaleServe(
    val hostname: String,
    val port: Int,
    val path: String,
    val proxy: String,
    val https: Boolean
)