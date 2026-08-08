package no.iktdev.kammich.sse.events

import no.iktdev.kammich.models.shared.network.WifiNetworkConnection
import no.iktdev.kammich.models.shared.network.WifiNetworkScan
import no.iktdev.kammich.models.shared.network.WifiNetworkTether
import no.iktdev.kammich.sse.ISSE

data class SSEWifiConnectivity(val payload: List<WifiNetworkConnection>): ISSE {
    override val type = "wifi-connectivity"
}

data class SSEWifiScan(val payload: List<WifiNetworkScan>): ISSE {
    override val type = "wifi-scan"
}

data class SSEWifiTethering(val payload:  WifiNetworkTether?): ISSE {
    override val type = "wifi-tethering"
}