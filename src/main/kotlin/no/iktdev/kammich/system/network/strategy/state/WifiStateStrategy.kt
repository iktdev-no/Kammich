package no.iktdev.kammich.system.network.strategy.state

import no.iktdev.kammich.models.shared.network.WifiInterfaceState
import no.iktdev.kammich.models.shared.network.WifiNetwork

interface WifiStateStrategy {
    fun getState(interfaceName: String): WifiInterfaceState

    fun getNetwork(interfaceName: String): WifiNetwork?
    fun isSupported(): Boolean
}