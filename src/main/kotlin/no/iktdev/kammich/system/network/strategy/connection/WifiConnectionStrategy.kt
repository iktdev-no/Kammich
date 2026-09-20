package no.iktdev.kammich.system.network.strategy.connection

import no.iktdev.kammich.models.shared.network.WifiConnection
import no.iktdev.kammich.models.shared.network.WifiInterfaceState
import no.iktdev.kammich.models.shared.network.WifiNetwork

interface WifiConnectionStrategy {
    fun connect(interfaceName: String, network: WifiNetwork, password: String?): WifiInterfaceState
    fun disconnect(interfaceName: String): WifiInterfaceState
    fun isSupported(): Boolean

    fun getState(ifName: String): WifiInterfaceState
    fun getNetwork(interfaceName: String): WifiNetwork?
}