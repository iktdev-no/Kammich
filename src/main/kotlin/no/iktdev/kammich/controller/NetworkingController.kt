package no.iktdev.kammich.controller

import no.iktdev.kammich.models.shared.network.NetworkInterface
import no.iktdev.kammich.system.network.NetworkInterfaceRegistryV2
import no.iktdev.kammich.system.network.NetworkingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/networking")
class NetworkingController(
    private val networkInterfaceRegistry: NetworkInterfaceRegistryV2,
    private val networkingService: NetworkingService
) {

    @GetMapping("/interfaces")
    fun interfaces(): List<NetworkInterface> {
        return networkInterfaceRegistry.listNetworkInterfaces()
    }

    @PostMapping("/interfaces/{interfaceName}/reset")
    fun reset(@PathVariable interfaceName: String) {
        return networkingService.reset(interfaceName)
    }
}