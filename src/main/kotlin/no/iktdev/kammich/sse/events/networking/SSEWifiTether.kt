package no.iktdev.kammich.sse.events.networking

import no.iktdev.kammich.models.shared.network.WifiTether
import no.iktdev.kammich.sse.ISSE

data class SSEWifiTether(val ifName: String, val payload: WifiTether?): ISSE {
    override val type: String = "wifi-tether"
}
