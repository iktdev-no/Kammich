package no.iktdev.kammich.system.network.wifi.strategy.connection

import no.iktdev.kammich.models.shared.network.WifiConnectionResult
import no.iktdev.kammich.models.shared.network.WifiInterfaceState
import no.iktdev.kammich.models.shared.network.WifiNetwork

interface WifiConnectionStrategy {
    fun connect(interfaceName: String, network: WifiNetwork, password: String?): WifiConnectionResult
    fun disconnect(interfaceName: String): WifiConnectionResult
    fun isSupported(): Boolean
    fun getState(interfaceName: String): WifiInterfaceState
}