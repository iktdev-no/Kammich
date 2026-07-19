package no.iktdev.kammich.controller

import no.iktdev.kammich.models.shared.network.WifiNetworkTether
import no.iktdev.kammich.models.shared.network.WirelessInterface
import no.iktdev.kammich.models.shared.network.WifiTetherAP
import no.iktdev.kammich.system.network.wifi.WifiTetherService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/wifi/tethering")
class WifiTetheringController(
    private val wifiTethering: WifiTetherService,
) {

    @GetMapping("/state")
    fun getState(): List<WifiNetworkTether> {
        return wifiTethering.getCurrentState()
    }

    @GetMapping("/interfaces")
    fun getInterfaces(): List<WirelessInterface> {
        return wifiTethering.getWifiTetheringInterfaces()
    }


    @PostMapping("/start/{interfaceName}")
    fun startTethering(@PathVariable interfaceName: String) {
        return wifiTethering.startTethering(interfaceName)
    }

    @PostMapping("/stop/{interfaceName}")
    fun stopTethering(@PathVariable interfaceName: String) {
        return wifiTethering.stopTethering(interfaceName)
    }

    @DeleteMapping("/remove")
    fun removeTethering(@RequestBody interfaceName: String) {
        return wifiTethering.removeTetherDevice(interfaceName)
    }

    @PostMapping("/add")
    fun addTethering(@RequestBody interfaceName: String) {
        return wifiTethering.saveTetherDevice(interfaceName)
    }

    @PostMapping("/ap")
    fun setApSettings(@RequestBody settings: WifiTetherAP) {
        return wifiTethering.saveTetherConfig(
            ssid = settings.ssid,
            password = settings.password,
            security = settings.security,
        )
    }

    @GetMapping("/ap")
    fun getApSettings(): WifiTetherAP? {
        return wifiTethering.getTetherSettings()
    }





}