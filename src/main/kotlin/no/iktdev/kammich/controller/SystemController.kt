package no.iktdev.kammich.controller

import no.iktdev.kammich.models.shared.system.ActionResponse
import no.iktdev.kammich.models.shared.system.PowerPermissionsDto
import no.iktdev.kammich.services.SystemPowerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/system")
class SystemController(
    private val systemPowerService: SystemPowerService
) {

    @GetMapping("/power-permissions")
    fun getPowerPermissions(): ResponseEntity<PowerPermissionsDto> {
        val permissions = PowerPermissionsDto(
            canPowerOff = systemPowerService.canPowerOff(),
            canReboot = systemPowerService.canReboot()
        )
        return ResponseEntity.ok(permissions)
    }

    @PostMapping("/poweroff")
    fun powerOff(): ResponseEntity<ActionResponse> {
        val success = systemPowerService.executePowerOff()
        if (success) {
            return ResponseEntity.ok(ActionResponse(true, "Systemet slås av..."))
        }
        return ResponseEntity.status(403).body(ActionResponse(false, "Mangler rettigheter eller feilet."))
    }

    @PostMapping("/reboot")
    fun reboot(): ResponseEntity<ActionResponse> {
        val success = systemPowerService.executeReboot()
        if (success) {
            return ResponseEntity.ok(ActionResponse(true, "Systemet starter på nytt..."))
        }
        return ResponseEntity.status(403).body(ActionResponse(false, "Mangler rettigheter eller feilet."))
    }
}