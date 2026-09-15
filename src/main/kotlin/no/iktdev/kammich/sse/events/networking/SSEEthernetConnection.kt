package no.iktdev.kammich.sse.events.networking

import no.iktdev.kammich.models.shared.network.EthernetConnection
import no.iktdev.kammich.sse.ISSE

data class SSEEthernetConnection(val ifName: String, val payload: EthernetConnection): ISSE {
    override val type: String = "ethernet-connect"
}