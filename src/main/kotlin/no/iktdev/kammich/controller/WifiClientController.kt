package no.iktdev.kammich.controller

import no.iktdev.kammich.models.shared.network.WifiInterfaceClient
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.system.network.WifiConnectionServiceV2
import no.iktdev.kammich.system.network.WifiScanServiceV2
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/wifi/client")
class WifiClientController(
    private val wifiScanner: WifiScanServiceV2,
    private val wifiConnector: WifiConnectionServiceV2,
) {

    @GetMapping("", "/")
    fun getInterfaces(): List<WifiInterfaceClient> {
        return wifiConnector.getCurrentState()
    }

    @GetMapping("/{interfaceName}/scan")
    fun getKnownNetworks(@PathVariable interfaceName: String): List<WifiNetwork> {
        return wifiScanner.getNetworks(interfaceName)
    }
    @PostMapping("/{interfaceName}/scan/start")
    fun startPeriodicallyScans(@PathVariable interfaceName: String){
        return wifiScanner.startPeriodicScan(interfaceName)
    }

    @PostMapping("/{interfaceName}/scan/stop")
    fun stopPeriodicallyScans(@PathVariable interfaceName: String) {
        return wifiScanner.stopPeriodicScan(interfaceName)
    }


    @PostMapping("/{interfaceName}/connect")
    fun connect(
        @PathVariable interfaceName: String,
        @RequestParam bssid: String,
        @RequestParam(required = false) password: String?
    ): ResponseEntity<Boolean> {
        return try {
            wifiConnector.connectAsync(interfaceName, bssid, password)
            ResponseEntity(true, HttpStatus.OK)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity(false, HttpStatus.NOT_ACCEPTABLE)
        }
    }

    @PostMapping("/{interfaceName}/disconnect")
    fun disconnect(
        @PathVariable interfaceName: String
    ): ResponseEntity<Boolean> {
        val success = wifiConnector.disconnect(interfaceName)
        val code = if (success) HttpStatus.OK else HttpStatus.BAD_REQUEST
        return ResponseEntity(success, code)
    }

}