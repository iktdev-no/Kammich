package no.iktdev.kammich.controller.networking

import no.iktdev.kammich.models.shared.network.EmergencyModeRequest
import no.iktdev.kammich.models.shared.network.EthernetInterfaceState
import no.iktdev.kammich.system.network.EthernetOperationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/ethernet")
class EthernetOperationController(
    private val ethernetService: EthernetOperationService
) {

    @GetMapping
    fun getAll(): List<EthernetInterfaceState> =
        ethernetService.getConnections()

    @GetMapping("/{interfaceName}")
    fun get(@PathVariable interfaceName: String): ResponseEntity<EthernetInterfaceState> =
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