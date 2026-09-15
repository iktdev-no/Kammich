package no.iktdev.kammich.controller

import no.iktdev.kammich.models.shared.ServiceStatus
import no.iktdev.kammich.system.StatusService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/status")
class StatusController(
    private val statusService: StatusService,
) {

    @GetMapping("/services")
    fun getServiceStatus(): ServiceStatus {
        return statusService.getServiceStatus()
    }

}