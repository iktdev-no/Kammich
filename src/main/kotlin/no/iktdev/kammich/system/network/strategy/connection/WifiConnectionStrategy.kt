package no.iktdev.kammich.system.network.strategy.connection

import no.iktdev.kammich.models.shared.network.WifiConnection
import no.iktdev.kammich.models.shared.network.WifiNetwork

interface WifiConnectionStrategy {
    fun connect(interfaceName: String, network: WifiNetwork, password: String?): WifiConnection
    fun disconnect(interfaceName: String): WifiConnection
    fun isSupported(): Boolean

    fun getState(ifName: String): WifiConnection
    fun getNetwork(interfaceName: String): WifiNetwork?
}