package no.iktdev.kammich.controller

import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WirelessInterface
import no.iktdev.kammich.system.network.wifi.WifiConnectivityService
import no.iktdev.kammich.system.network.wifi.WifiScanner
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/wifi/client")
class WifiClientController(
    private val wifiScanner: WifiScanner,
    private val wifiConnector: WifiConnectivityService,
) {

    @GetMapping("/interfaces")
    fun getInterfaces(): List<WirelessInterface> {
        return wifiConnector.getWifiClientInterfaces()
    }

    @GetMapping("/{interfaceName}/scan")
    fun getKnownNetworks(@PathVariable interfaceName: String): List<WifiNetwork> {
        return wifiScanner.getNetworks(interfaceName, false)
    }
    @PostMapping("/{interfaceName}/scan")
    fun getNetworks(@PathVariable interfaceName: String): List<WifiNetwork> {
        return wifiScanner.getNetworks(interfaceName, true)
    }

    @PostMapping("/{interfaceName}/connect")
    fun connect(
        @PathVariable interfaceName: String,
        @RequestParam bssid: String,
        @RequestParam(required = false) password: String?
    ): ResponseEntity<Boolean> {
        val result = wifiConnector.connectToNetwork(interfaceName, bssid, password)
        return if (result) {
            ResponseEntity(true, HttpStatus.OK)
        } else ResponseEntity(false, HttpStatus.NOT_ACCEPTABLE)
    }

    @PostMapping("/{interfaceName}/disconnect")
    fun disconnect(
        @PathVariable interfaceName: String
    ): ResponseEntity<Boolean> {
        val success = wifiConnector.disconnectFromNetwork(interfaceName)
        val code = if (success) HttpStatus.OK else HttpStatus.BAD_REQUEST
        return ResponseEntity(success, code)
    }

}