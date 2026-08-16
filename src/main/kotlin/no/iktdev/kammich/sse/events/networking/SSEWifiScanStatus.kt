package no.iktdev.kammich.sse.events.networking

import no.iktdev.kammich.models.shared.network.WifiScanStatus
import no.iktdev.kammich.sse.ISSE

data class SSEWifiScanStatus(val state: WifiScanStatus): ISSE {
    override val type: String = "wifi-scan-status"
}