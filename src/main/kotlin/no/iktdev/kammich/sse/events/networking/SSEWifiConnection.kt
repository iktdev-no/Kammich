package no.iktdev.kammich.sse.events.networking

import no.iktdev.kammich.models.shared.network.WifiConnection
import no.iktdev.kammich.sse.ISSE

data class SSEWifiConnection(val ifName: String, val payload: WifiConnection?): ISSE {
    override val type: String = "wifi-connect"
}