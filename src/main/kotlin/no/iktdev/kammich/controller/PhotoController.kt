package no.iktdev.kammich.controller

import no.iktdev.kammich.models.shared.PagedResponse
import no.iktdev.kammich.models.shared.RemoteFile
import no.iktdev.kammich.storage.media.PhotoService
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/photo")
class PhotoController(
    private val photoService: PhotoService,
) {
    @GetMapping
    fun getPhotos(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): PagedResponse<RemoteFile> {
        val (files, total) = photoService.getPagedFiles(page, size, null)
        return PagedResponse(
            data = files,
            currentPage = page,
            totalPages = (total / size).toInt(),
            hasMore = (page * size) + files.size < total
        )
    }

    @GetMapping("/{deviceId}")
    fun getPhotosForDevice(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @PathVariable deviceId: String,
    ): PagedResponse<RemoteFile> {
        val (files, total) = photoService.getPagedFiles(page, size, deviceId)
        return PagedResponse(
            data = files,
            currentPage = page,
            totalPages = (total / size).toInt(),
            hasMore = (page * size) + files.size < total
        )
    }

    @GetMapping("/{deviceId}/{fileName:.+}")
    fun getMediaStream(
        @PathVariable deviceId: Int,
        @PathVariable fileName: String
    ): ResponseEntity<Resource> {
        // Her kan du nå enkelt hente ut selve bildet/filen
        val file = photoService.getFile(deviceId, fileName)
        return ResponseEntity.ok().body(file)
    }

}