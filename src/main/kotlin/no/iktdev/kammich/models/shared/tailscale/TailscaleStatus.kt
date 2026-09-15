package no.iktdev.kammich.models.shared.tailscale

data class TailscaleStatus(
    val version: String,
    val tun: Boolean,
    val backendState: String,
    val self: TailscaleStatusSelf
)

data class TailscaleStatusSelf(
    val online: Boolean,
    val exitNode: Boolean,
    val exitNodeOption: Boolean,
    val relay: String,
    val dnsName: String
)