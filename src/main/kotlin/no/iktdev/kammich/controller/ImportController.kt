package no.iktdev.kammich.controller

import no.iktdev.kammich.importing.ImportService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/import")
class ImportController(
    private val importService: ImportService,
) {


    @PostMapping("/{deviceId}/cancel")
    fun cancelImport(@PathVariable deviceId: String) {
        importService.cancelImport(deviceId)
    }
}