package no.iktdev.kammich.controller

import no.iktdev.kammich.models.shared.Version
import no.iktdev.kammich.services.UpdateService
import no.iktdev.kammich.services.VersionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/update")
class UpdateController(
    private val updateService: UpdateService,
    private val versionService: VersionService
) {

    @GetMapping
    fun getVersion(): Version =
        versionService.getVersion()

    @PostMapping
    fun update(): ResponseEntity<Unit> {
        return if (updateService.startUpdate()) {
            ResponseEntity.accepted().build()
        } else {
            ResponseEntity.status(409).build()
        }
    }
}