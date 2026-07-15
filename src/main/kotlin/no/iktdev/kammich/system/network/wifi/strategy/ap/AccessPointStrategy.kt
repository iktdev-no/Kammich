package no.iktdev.kammich.system.network.wifi.strategy.ap


import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiTetherSetting
import no.iktdev.kammich.models.shared.network.WifiTetheringNetwork
import no.iktdev.kammich.models.shared.network.WifiTetheringState
import no.iktdev.kammich.system.network.wifi.WifiRunner

interface AccessPointStrategy {
    fun start(interfaceName: String, tether: WifiTetherSetting): WifiRunner.CommandResult
    fun startAligned(interfaceName: String, tether: WifiTetherSetting, network: WifiNetwork): WifiTetheringNetwork?
    fun stop(interfaceName: String): Boolean
    fun isSupported(): Boolean
    fun getActiveTethering(interfaceName: String): WifiTetheringNetwork?
    fun getTetheringStatus(interfaceName: String): WifiTetheringState
}