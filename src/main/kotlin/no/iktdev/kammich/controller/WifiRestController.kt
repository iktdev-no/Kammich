package no.iktdev.kammich.controller

import no.iktdev.kammich.models.shared.network.WifiConnectionResult
import no.iktdev.kammich.models.shared.network.WifiInterface
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiInterfaceState
import no.iktdev.kammich.models.shared.network.WifiTetherInterface
import no.iktdev.kammich.models.shared.network.WifiTetherSetting
import no.iktdev.kammich.system.network.wifi.WifiConnectivityService
import no.iktdev.kammich.system.network.wifi.WifiInterfaces
import no.iktdev.kammich.system.network.wifi.WifiScanner
import no.iktdev.kammich.system.network.wifi.WifiTetherService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/wifi")
class WifiRestController(
    private val wifiInterfaces: WifiInterfaces,
    private val wifiScanner: WifiScanner,
    private val wifiService: WifiConnectivityService,
    private val wifiTethering: WifiTetherService
) {

    private val log = LoggerFactory.getLogger(WifiRestController::class.java)


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
                supportsSimultaneousApSta = internal.supportsApAndStationSimultaneously,
                deviceId = internal.deviceId,
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
        @RequestParam bssid: String,
        @RequestParam(required = false) password: String?
    ): WifiConnectionResult {
        return wifiService.connectToNetwork(interfaceName, bssid, password)
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

    @GetMapping("/tether/config/ap")
    fun getTetherConfig(): ResponseEntity<WifiTetherSetting> {
        val tether = wifiTethering.getTetherSettings()
        return ResponseEntity.ok(tether)
    }

    @GetMapping("/tether/config/devices")
    fun getTetherDevices(): ResponseEntity<List<WifiTetherInterface>> {
        val tether = wifiTethering.getAvailableTetherDevices()
        return ResponseEntity.ok(tether)
    }

    @PostMapping("/tether/config/ap")
    fun setTetherConfig(@RequestBody setting: WifiTetherSetting): ResponseEntity<Boolean> {
        wifiTethering.saveTetherConfig(setting.ssid, setting.password, setting.security)
        return ResponseEntity.ok(true)
    }

    @PostMapping("/tether/config/device/split")
    fun setTetherDeviceSplit(@RequestBody deviceId: String): ResponseEntity<Boolean> {
        log.info("Splitting and setting new Tether device ID: $deviceId")
        wifiTethering.splitDevice(deviceId)
        return ResponseEntity.ok(true)
    }

    @PostMapping("/tether/config/device/use")
    fun setTetherDevice(@RequestBody deviceId: String): ResponseEntity<Boolean> {
        log.info("Setting new Tether device ID: $deviceId")
        wifiTethering.saveTetherDevice(true, deviceId)
        return ResponseEntity.ok(true)
    }

    @DeleteMapping("/tether/config/device/use")
    fun deleteTetherDevice(@RequestBody deviceId: String): ResponseEntity<Boolean> {
        log.info("Deleting Tether device ID: $deviceId")
        wifiTethering.removeTetherDevice(deviceId)
        return ResponseEntity.ok(true)
    }

    @PostMapping("/tether/start")
    fun startTether(): ResponseEntity<Unit> {
        return try {
            wifiTethering.startTethering()
            ResponseEntity.ok().build()
        } catch (ex: Exception) {
            log.error("Error starting Tether", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping("/tether/stop")
    fun stopTether(): ResponseEntity<Unit> {
        return try {
            wifiTethering.stopTethering()
            ResponseEntity.ok().build()
        } catch (ex: Exception) {
            log.info("Could not stop Tether", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

}