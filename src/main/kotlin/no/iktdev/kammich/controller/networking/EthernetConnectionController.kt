package no.iktdev.kammich.controller.networking

import no.iktdev.kammich.models.shared.network.EmergencyModeRequest
import no.iktdev.kammich.models.shared.network.EthernetConnection
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode
import no.iktdev.kammich.system.network.EthernetConnectionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/ethernet")
class EthernetConnectionController(
    private val ethernetService: EthernetConnectionService
) {

    @GetMapping
    fun getAll(): List<EthernetConnection> =
        ethernetService.getConnections()

    @GetMapping("/{interfaceName}")
    fun get(@PathVariable interfaceName: String): ResponseEntity<EthernetConnection> =
        ethernetService.getConnection(interfaceName)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @PostMapping("/{interfaceName}/client")
    fun startClient(@PathVariable interfaceName: String): ResponseEntity<Void> =
        if (ethernetService.startClient(interfaceName))
            ResponseEntity.ok().build()
        else
            ResponseEntity.internalServerError().build()

    @PostMapping("/{interfaceName}/tether")
    fun startTether(@PathVariable interfaceName: String): ResponseEntity<Void> =
        if (ethernetService.startTether(interfaceName))
            ResponseEntity.ok().build()
        else
            ResponseEntity.internalServerError().build()

    @PostMapping("/{interfaceName}/disconnect")
    fun disconnect(@PathVariable interfaceName: String): ResponseEntity<Void> =
        if (ethernetService.disconnect(interfaceName))
            ResponseEntity.ok().build()
        else
            ResponseEntity.internalServerError().build()

    @PostMapping("/{interfaceName}/reset")
    fun reset(@PathVariable interfaceName: String): ResponseEntity<Void> =
        if (ethernetService.reset(interfaceName))
            ResponseEntity.ok().build()
        else
            ResponseEntity.internalServerError().build()


    @PutMapping("/{interfaceName}/emergency")
    fun setEmergency(
        @PathVariable interfaceName: String,
        @RequestBody request: EmergencyModeRequest
    ): ResponseEntity<Void> =
        if (ethernetService.setEmergencyAllowed(interfaceName, request.enabled))
            ResponseEntity.ok().build()
        else
            ResponseEntity.notFound().build()
}