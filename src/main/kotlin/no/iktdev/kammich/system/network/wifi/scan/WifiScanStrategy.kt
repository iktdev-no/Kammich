package no.iktdev.kammich.system.network.wifi.scan

import no.iktdev.kammich.models.internal.network.WifiScanResult
import no.iktdev.kammich.models.shared.network.WifiNetwork

interface WifiScanStrategy {
    fun scan(interfaceName: String): WifiScanResult
    fun isSupported(): Boolean
}