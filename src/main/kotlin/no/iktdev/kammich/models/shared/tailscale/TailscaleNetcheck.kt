package no.iktdev.kammich.models.shared.tailscale

import java.time.Duration
import java.time.Instant

data class TailscaleNetcheck(
    val now: Instant,
    val udp: Boolean,
    val ipv6: Boolean,
    val ipv4: Boolean,
    val ipv6CanSend: Boolean,
    val ipv4CanSend: Boolean,
    val osHasIpv6: Boolean,
    val icmpv4: Boolean,
    val mappingVariesByDestIp: Boolean,
    val upnp: Boolean,
    val pmp: Boolean,
    val pcp: Boolean,
    val preferredDerp: Int?,
    val regionLatency: Map<Int, Duration>,
    val regionV4Latency: Map<Int, Duration>,
    val regionV6Latency: Map<Int, Duration>,
    val globalV4: String?,
    val globalV6: String?,
    val captivePortal: String?
)