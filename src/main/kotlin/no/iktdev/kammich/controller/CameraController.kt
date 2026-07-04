package no.iktdev.kammich.controller

import jakarta.servlet.http.HttpServletRequest
import no.iktdev.kammich.models.files.KFile
import no.iktdev.kammich.models.storage.removable.Device
import no.iktdev.kammich.models.storage.removable.DeviceInfo
import no.iktdev.kammich.storage.DeviceManagerService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.HandlerMapping

@RestController
@RequestMapping("/api/camera")
class CameraController(private val deviceManager: DeviceManagerService) {

    @GetMapping("/{port}")
    fun getDeviceInfo(@PathVariable("port") port: String): DeviceInfo? {
        return deviceManager.getDeviceInfo(port)
    }

    @GetMapping("/{port}/files/**")
    fun getFiles(
        @PathVariable port: String,
        request: HttpServletRequest // Trenger denne for å hente ut resten av stien
    ): List<KFile> {
        val device = deviceManager.getDevice(port) ?: throw IllegalStateException("Device not found: $port")

        // Hent ut stien etter "/files/"
        val fullPath = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) as String
        val subPath = fullPath.substringAfter("/files/", "/")

        return deviceManager.getFilesForDevice(device, subPath)
    }
}