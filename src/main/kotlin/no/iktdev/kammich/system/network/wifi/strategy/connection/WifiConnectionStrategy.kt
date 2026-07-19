package no.iktdev.kammich.system.network.wifi.strategy.connection

import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiNetworkConnection

interface WifiConnectionStrategy {
    fun connect(interfaceName: String, network: WifiNetwork, password: String?): WifiNetworkConnection
    fun disconnect(interfaceName: String): WifiNetworkConnection
    fun isSupported(): Boolean
    fun getState(interfaceName: String): WifiNetworkConnection
}