package no.iktdev.kammich.controller

import no.iktdev.kammich.services.importing.MockImportService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/mock")
class MockImportController(
    private val mockImportService: MockImportService
) {

    @GetMapping("/import", "/import/{files}")
    fun triggerImport(@PathVariable files: Int?): ResponseEntity<String> {
        val msg = if (files != null) "Starter mock-import med $files filer." else "Starter mock-import med tilfeldig antall filer."
        mockImportService.mockImportOf(files)
        return ResponseEntity.ok(msg)
    }
}