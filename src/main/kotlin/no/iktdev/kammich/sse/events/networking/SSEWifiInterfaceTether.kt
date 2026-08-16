package no.iktdev.kammich.sse.events.networking

import no.iktdev.kammich.models.shared.network.WifiInterfaceTether
import no.iktdev.kammich.sse.ISSE

class SSEWifiInterfaceTether(val payload: List<WifiInterfaceTether>): ISSE {
    override val type: String = "wifi-interface-tether"
}