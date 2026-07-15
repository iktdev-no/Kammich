package no.iktdev.kammich.system.network.wifi.strategy.scan

import no.iktdev.kammich.models.internal.network.WifiScanResult

interface WifiScanStrategy {
    fun scan(interfaceName: String): WifiScanResult
    fun isSupported(): Boolean
}