package no.iktdev.kammich.models.internal.network

import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiScanState

data class WifiScanResult(
    val state: WifiScanState,
    val success: Boolean,
    val message: String? = null,
    val networks: List<WifiNetwork>
)