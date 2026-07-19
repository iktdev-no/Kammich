package no.iktdev.kammich.system.network.wifi.strategy.scan

import no.iktdev.kammich.models.internal.network.WifiScanState

interface WifiScanStrategy {
    fun scan(interfaceName: String): WifiScanState
    fun isSupported(): Boolean
}