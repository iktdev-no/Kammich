package no.iktdev.kammich.system.network.wifi.strategy.ap


import no.iktdev.kammich.models.shared.network.WifiNetworkTether
import no.iktdev.kammich.models.shared.network.WifiTetherAP
import no.iktdev.kammich.system.SysCommand

interface AccessPointStrategy {
    fun start(interfaceName: String, tether: WifiTetherAP): SysCommand.Result
    fun stop(interfaceName: String): Boolean
    fun isSupported(): Boolean
    fun getState(interfaceName: String): WifiNetworkTether
}