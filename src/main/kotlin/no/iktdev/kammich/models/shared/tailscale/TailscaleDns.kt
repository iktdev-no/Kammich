package no.iktdev.kammich.models.shared.tailscale

data class TailscaleDns(
    val tailscaleDns: Boolean,
    val magicDnsEnabled: Boolean,
    val magicDnsSuffix: String?,
    val selfDnsName: String?,
    val splitDnsRoutes: Map<String, List<TailscaleDnsServer>>,
    val searchDomains: List<String>,
    val certDomains: List<String>,
    val exitNodeFilteredSet: List<String>
)

data class TailscaleDnsServer(
    val address: String
)