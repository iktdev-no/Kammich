package no.iktdev.kammich.system.network.wifi.strategy.connection

import no.iktdev.kammich.models.shared.network.WifiConnectionResult
import no.iktdev.kammich.models.shared.network.WifiInterfaceState

interface WifiConnectionStrategy {
    fun connect(interfaceName: String, ssid: String, password: String?): WifiConnectionResult
    fun disconnect(interfaceName: String): WifiConnectionResult
    fun isSupported(): Boolean
    fun getState(interfaceName: String): WifiInterfaceState
}