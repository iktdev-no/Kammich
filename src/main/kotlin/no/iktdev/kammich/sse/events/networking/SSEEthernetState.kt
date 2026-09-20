package no.iktdev.kammich.sse.events.networking

import no.iktdev.kammich.models.shared.network.EthernetInterfaceState
import no.iktdev.kammich.sse.ISSE

data class SSEEthernetState(val ifName: String, val payload: EthernetInterfaceState): ISSE {
    override val type: String = "ethernet-state"
}