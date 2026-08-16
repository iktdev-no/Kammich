package no.iktdev.kammich.models.internal.network

import no.iktdev.kammich.models.shared.network.WifiNetwork
import java.time.ZonedDateTime


data class WifiScanState(
    val networks: List<WifiNetwork> = emptyList(),
    val performedAt: ZonedDateTime = ZonedDateTime.now(),
    val error: String? = null,
)

