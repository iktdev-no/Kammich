package no.iktdev.kammich.controller

import no.iktdev.kammich.importing.ImportService
import no.iktdev.kammich.models.shared.DeviceImport
import no.iktdev.kammich.repository.FileRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/import")
class ImportController(
    private val importService: ImportService,
    private val fileRepository: FileRepository
) {

    @GetMapping
    fun getAllActiveImports(): List<DeviceImport> {
        return importService.getAllActiveImports()
    }

    @GetMapping("/history")
    fun getImportHistory(): List<DeviceImport> {
        return fileRepository.getImportHistory()

    }

    @PostMapping("/cancel/all")
    fun cancelImport() {
        importService.cancelAllImports()
    }
    @PostMapping("/cancel/device/{deviceId}")
    fun cancelImportFor(@PathVariable deviceId: String) {
        importService.cancelImport(deviceId)
    }
}