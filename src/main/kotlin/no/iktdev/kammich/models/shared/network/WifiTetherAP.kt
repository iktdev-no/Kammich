package no.iktdev.kammich.models.shared.network

import no.iktdev.kammich.models.shared.network.old.WifiSecurityType

data class WifiTetherAP(
    val ssid: String,
    val password: String,
    val security: WifiSecurityType = WifiSecurityType.WPA2
)