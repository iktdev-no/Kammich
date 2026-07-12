package no.iktdev.kammich.controller

import jakarta.servlet.http.HttpServletRequest
import no.iktdev.kammich.models.shared.DeviceSettingsDto
import no.iktdev.kammich.models.shared.WFile
import no.iktdev.kammich.models.shared.device.DeviceInfo
import no.iktdev.kammich.storage.DeviceManagerService
import no.iktdev.kammich.storage.FilesService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.HandlerMapping

@RestController
@RequestMapping("/api/v1/camera")
class CameraController(
    private val deviceManager: DeviceManagerService,
    private val fileService: FilesService
) {

    @GetMapping("/{deviceId}")
    fun getDeviceInfo(@PathVariable("deviceId") port: String): DeviceInfo? {
        return deviceManager.getDeviceInfo(port)
    }

    @GetMapping("/{deviceId}/files/**")
    fun getFiles(
        @PathVariable deviceId: String,
        request: HttpServletRequest // Trenger denne for å hente ut resten av stien
    ): List<WFile> {
        val device = deviceManager.getDevice(deviceId) ?: throw IllegalStateException("Device not found: $deviceId")

        // Hent ut stien etter "/files/"
        val fullPath = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) as String
        val subPath = fullPath.substringAfter("/files/", "/")

        return fileService.getFilesForDevice(device, subPath)
    }

    @PatchMapping("/{deviceId}")
    fun updateSettings(
        @PathVariable deviceId: String,
        @RequestBody settings: DeviceSettingsDto
    ): DeviceInfo {
        val device = deviceManager.getDevice(deviceId)
            ?: throw IllegalStateException("Device not found: $deviceId")

        settings.autoImport?.let { deviceManager.setAutoImport(device, it) }

        // Her overskriver vi listene med de nye verdiene frontend sender
        settings.includeFolders?.let { deviceManager.setIncludeFolders(device, it) }
        settings.excludeFolders?.let { deviceManager.setExcludeFolders(device, it) }

        return deviceManager.getDeviceInfo(deviceId)!!
    }

}