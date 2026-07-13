package no.iktdev.kammich.controller

import no.iktdev.kammich.models.shared.network.WifiConnectionResult
import no.iktdev.kammich.models.shared.network.WifiInterface
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiInterfaceState
import no.iktdev.kammich.system.network.wifi.WifiConnectivityService
import no.iktdev.kammich.system.network.wifi.WifiInterfaces
import no.iktdev.kammich.system.network.wifi.WifiScanner
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/wifi")
class WifiRestController(
    private val wifiInterfaces: WifiInterfaces,
    private val wifiScanner: WifiScanner,
    private val wifiService: WifiConnectivityService
) {

    @GetMapping("/state")
    fun getWifiState(): List<WifiInterfaceState> {
        return wifiService.getAllNetworkStates()
    }

    @GetMapping("/connection")
    fun getWifiConnections(): List<WifiInterfaceState> {
        return wifiService.getAllNetworkStates()
    }

    /**
     * Returnerer et rent FE-objekt isolert fra interne 'jc'-strukturer
     */
    @GetMapping("/interfaces")
    fun getInterfaces(): List<WifiInterface> {
        return wifiInterfaces.getInterfaces().map { internal ->
            WifiInterface(
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
    ): List<WifiNetwork> {
        return wifiScanner.getNetworks(interfaceName, forceRescan = force)
            .filter { it.ssid.isNotBlank() }
    }

    @PostMapping("/connect")
    fun connect(
        @RequestParam interfaceName: String,
        @RequestParam ssid: String,
        @RequestParam(required = false) password: String?
    ): WifiConnectionResult {
        return wifiService.connectToNetwork(interfaceName, ssid, password)
    }

    @PostMapping("/disconnect")
    fun disconnect(
        @RequestParam interfaceName: String
    ): WifiConnectionResult {
        return wifiService.disconnectFromNetwork(interfaceName)
    }

    @PostMapping("/scan")
    fun startScan(@RequestParam interfaceName: String): ResponseEntity<Unit> {
        wifiScanner.triggerScanAsync(interfaceName)
        return ResponseEntity.status(HttpStatus.ACCEPTED).build()
    }
}