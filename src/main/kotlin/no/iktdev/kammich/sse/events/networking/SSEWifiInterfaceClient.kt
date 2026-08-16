package no.iktdev.kammich.sse.events.networking

import no.iktdev.kammich.models.shared.network.WifiConnection
import no.iktdev.kammich.models.shared.network.WifiInterfaceClient
import no.iktdev.kammich.models.shared.network.WifiInterfaceTether
import no.iktdev.kammich.sse.ISSE

class SSEWifiInterfaceClient(val payload: List<WifiInterfaceClient>): ISSE {
    override val type: String = "wifi-interface-client"
}