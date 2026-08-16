package no.iktdev.kammich.sse.events.networking

import no.iktdev.kammich.models.shared.network.WifiScanResult
import no.iktdev.kammich.sse.ISSE

data class SSEWifiScanResult(val payload: WifiScanResult): ISSE {
    override val type: String = "wifi-scan-result"
}
