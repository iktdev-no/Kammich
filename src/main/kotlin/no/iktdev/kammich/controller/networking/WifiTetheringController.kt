package no.iktdev.kammich.controller.networking

import no.iktdev.kammich.models.shared.network.WifiInterfaceState
import no.iktdev.kammich.models.shared.network.WifiTetherAP
import no.iktdev.kammich.system.network.WifiOperationService
import no.iktdev.kammich.system.network.wifi.WifiTetherServiceV2
import org.springframework.http.ResponseEntity
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
    private val wifiOperationService: WifiOperationService
) {

    @GetMapping("", "/")
    fun getState(): List<WifiInterfaceState> {
        return wifiOperationService.getAll()
    }

    @GetMapping("/{interfaceName}")
    fun get(@PathVariable interfaceName: String): ResponseEntity<WifiInterfaceState> =
        wifiOperationService.getConnection(interfaceName)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @PostMapping("/start/{interfaceName}")
    fun startTethering(
        @PathVariable interfaceName: String
    ) {
        wifiOperationService.onStartTether(interfaceName)
    }

    @PostMapping("/stop/{interfaceName}")
    fun stopTethering(
        @PathVariable interfaceName: String
    ) {
        wifiOperationService.onStopTether(interfaceName)
    }

    @DeleteMapping("/release")
    fun releaseTetheringDevice(
        @RequestBody interfaceName: String
    ): Boolean {
        return wifiTethering.releaseTetherDevice(interfaceName)
    }

    @PostMapping("/use")
    fun useTetherDevice(
        @RequestBody interfaceName: String
    ): Boolean {
        return wifiTethering.acquireTetherDevice(interfaceName)
    }

    @PostMapping("/ap")
    fun setApSettings(
        @RequestBody settings: WifiTetherAP
    ) {
        wifiTethering.saveTetherConfig(
            ssid = settings.ssid,
            password = settings.password,
            security = settings.security,
        )
    }

    @GetMapping("/ap")
    fun getApSettings(): WifiTetherAP {
        return wifiTethering.getTetherSettings()
    }
}