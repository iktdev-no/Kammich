package no.iktdev.kammich.controller.networking

import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode
import no.iktdev.kammich.models.shared.network.WifiInterfaceState
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.system.network.WifiOperationService
import no.iktdev.kammich.system.network.wifi.WifiConnectionServiceV2
import no.iktdev.kammich.system.network.wifi.WifiScanServiceV2
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/wifi")
class WifiOperationController(
    private val wifiOperationService: WifiOperationService,
    private val wifiScanner: WifiScanServiceV2,
) {

    @GetMapping("", "/")
    fun getInterfaces(): List<WifiInterfaceState> {
        return wifiOperationService.getAll()
    }


    @PostMapping("/{interfaceName}/reset")
    fun reset(@PathVariable interfaceName: String) {
        return wifiOperationService.reset(interfaceName)
    }

    @PostMapping("/{interfaceName}/release")
    fun release(@PathVariable interfaceName: String) {
        wifiOperationService.onReleaseLease(interfaceName)
    }

    @GetMapping("/client/{interfaceName}")
    fun get(@PathVariable interfaceName: String): ResponseEntity<WifiInterfaceState> =
        wifiOperationService.getConnection(interfaceName)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @PostMapping("/client/use")
    fun useClientDevice(@RequestBody interfaceName: String): Boolean {
        return wifiOperationService.acquireDevice(interfaceName, NetworkInterfaceMode.Client)
    }

    @GetMapping("/client/{interfaceName}/scan")
    fun getKnownNetworks(
        @PathVariable interfaceName: String
    ): List<WifiNetwork> {
        return wifiScanner.getNetworks(interfaceName)
    }

    @PostMapping("/client/{interfaceName}/scan/start")
    fun startPeriodicallyScans(
        @PathVariable interfaceName: String
    ) {
        wifiScanner.startPeriodicScan(interfaceName)
    }

    @PostMapping("/client/{interfaceName}/scan/stop")
    fun stopPeriodicallyScans(
        @PathVariable interfaceName: String
    ) {
        wifiScanner.stopPeriodicScan(interfaceName)
    }

    @PostMapping("/client/{interfaceName}/connect")
    fun connect(
        @PathVariable interfaceName: String,
        @RequestParam bssid: String,
        @RequestParam(required = false) password: String?
    ): ResponseEntity<Boolean> {
        return try {
            wifiOperationService.onConnectClient(interfaceName, bssid, password)
            ResponseEntity(true, HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity(false, HttpStatus.NOT_ACCEPTABLE)
        }
    }

    @PostMapping("/client/{interfaceName}/disconnect")
    fun disconnect(
        @PathVariable interfaceName: String
    ): ResponseEntity<Boolean> {
        return try {
            wifiOperationService.onDisconnectClient(interfaceName)
            ResponseEntity(true, HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity(false, HttpStatus.NOT_ACCEPTABLE)
        }
    }
}