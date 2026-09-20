package no.iktdev.kammich.sse.events.networking

import no.iktdev.kammich.models.shared.network.WifiInterfaceState
import no.iktdev.kammich.sse.ISSE

data class SSEWifiState(val ifName: String, val payload: WifiInterfaceState): ISSE {
    override val type: String = "wifi-state"
}