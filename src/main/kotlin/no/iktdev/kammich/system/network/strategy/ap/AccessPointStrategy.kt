package no.iktdev.kammich.system.network.strategy.ap


import no.iktdev.kammich.models.shared.network.WifiTetherAP
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiTether

interface AccessPointStrategy {
    fun start(interfaceName: String, tether: WifiTetherAP, autoconnect: Boolean): WifiTether
    fun stop(interfaceName: String): Boolean
    fun isSupported(): Boolean
    fun getState(interfaceName: String): WifiTether
    fun getNetwork(interfaceName: String): WifiNetwork?
}