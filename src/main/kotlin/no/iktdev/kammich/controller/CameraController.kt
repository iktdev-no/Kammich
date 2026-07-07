package no.iktdev.kammich.controller

import jakarta.servlet.http.HttpServletRequest
import no.iktdev.kammich.models.shared.files.KFile
import no.iktdev.kammich.models.shared.storage.removable.DeviceInfo
import no.iktdev.kammich.storage.DeviceManagerService
import no.iktdev.kammich.storage.FilesService
import no.iktdev.kammich.storage.media.PhotoService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.HandlerMapping

@RestController
@RequestMapping("/api/camera")
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
    ): List<KFile> {
        val device = deviceManager.getDevice(deviceId) ?: throw IllegalStateException("Device not found: $deviceId")

        // Hent ut stien etter "/files/"
        val fullPath = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) as String
        val subPath = fullPath.substringAfter("/files/", "/")

        return fileService.getFilesForDevice(device, subPath)
    }
}