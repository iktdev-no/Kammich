package no.iktdev.kammich.controller

import no.iktdev.kammich.models.shared.network.WifiInterfaceTether
import no.iktdev.kammich.models.shared.network.WifiTetherAP
import no.iktdev.kammich.system.network.WifiTetherServiceV2
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
    private val wifiTethering: WifiTetherServiceV2,
) {

    @GetMapping("", "/")
    fun getState(): List<WifiInterfaceTether> {
        return wifiTethering.getCurrentState()
    }

    @PostMapping("/start/{interfaceName}")
    fun startTethering(@PathVariable interfaceName: String) {
        return wifiTethering.startTetheringAsync(interfaceName)
    }

    @PostMapping("/stop/{interfaceName}")
    fun stopTethering(@PathVariable interfaceName: String) {
        wifiTethering.stopTethering(interfaceName)
    }

    @DeleteMapping("/release")
    fun releaseTetheringDevice(@RequestBody interfaceName: String): Boolean {
        return wifiTethering.releaseTetherDevice(interfaceName)
    }

    @PostMapping("/use")
    fun useTetherDevice(@RequestBody interfaceName: String): Boolean {
        return wifiTethering.acquireTetherDevice(interfaceName)
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