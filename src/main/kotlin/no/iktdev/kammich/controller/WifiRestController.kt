package no.iktdev.kammich.controller

import no.iktdev.kammich.models.shared.network.ConnectionResult
import no.iktdev.kammich.models.shared.network.FeWifiInterface
import no.iktdev.kammich.models.shared.network.FeWifiNetwork
import no.iktdev.kammich.models.shared.network.WifiActivityState
import no.iktdev.kammich.system.network.WifiCommandService
import no.iktdev.kammich.system.network.WifiInterfaces
import no.iktdev.kammich.utils.WifiUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/wifi")
class WifiRestController(
    private val wifiInterfaces: WifiInterfaces,
    private val wifiService: WifiCommandService
) {

    @GetMapping("/state")
    fun getWifiState(): WifiActivityState {
        return wifiService.getCurrentState()
    }

    /**
     * Returnerer et rent FE-objekt isolert fra interne 'jc'-strukturer
     */
    @GetMapping("/interfaces")
    fun getInterfaces(): List<FeWifiInterface> {
        return wifiInterfaces.getInterfaces().map { internal ->
            FeWifiInterface(
                name = internal.interfaceName,
                supportsAp = internal.supportsAp,
                supportsSimultaneousApSta = internal.supportsApAndStationSimultaneously
            )
        }
    }

    /**
     * Returnerer en strømlinjeformet nettverksliste der rå dBm er regnet om til prosent (0-100%)
     */
    @GetMapping("/networks/{interfaceName}")
    fun getAvailableNetworks(
        @PathVariable interfaceName: String,
        @RequestParam(defaultValue = "false") force: Boolean
    ): List<FeWifiNetwork> {
        return wifiService.getNetworks(interfaceName, forceRescan = force)
            .filter { it.ssid.isNotBlank() }
    }

    @PostMapping("/connect")
    fun connect(
        @RequestParam interfaceName: String,
        @RequestParam ssid: String,
        @RequestParam(required = false) password: String?
    ): ConnectionResult {
        return wifiService.connectToNetwork(interfaceName, ssid, password)
    }

    @PostMapping("/scan")
    fun startScan(@RequestParam interfaceName: String): ResponseEntity<Unit> {
        wifiService.triggerScanAsync(interfaceName)
        return ResponseEntity.status(HttpStatus.ACCEPTED).build()
    }
}