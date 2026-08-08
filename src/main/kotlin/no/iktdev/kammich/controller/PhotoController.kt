package no.iktdev.kammich.controller

import no.iktdev.kammich.models.shared.PagedResponse
import no.iktdev.kammich.models.shared.RemoteFile
import no.iktdev.kammich.models.shared.device.PhotoDevice
import no.iktdev.kammich.storage.media.PhotoService
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest
import java.time.Duration

@RestController
@RequestMapping("/api/v1/photo")
class PhotoController(
    private val photoService: PhotoService,
) {
    @GetMapping("", "/{deviceId}")
    fun getPhotos(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @PathVariable(required = false) deviceId: String?,
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
        @PathVariable fileName: String,
        request: WebRequest
    ): ResponseEntity<Resource> {
        // 1. Hent filen (eventuelt cache serial-oppslaget i et lite minne/map om du vil optimere enda mer)
        val fileResource = photoService.getFile(deviceId, fileName)
        val file = fileResource.file

        // 2. Bruk filens sist endret-tidspunkt og størrelse som et unikt ETag/LastModified-stempel
        val lastModified = file.lastModified()
        val length = file.length()

        // 3. Hvis nettleseren har den i cachen, returner 304 Not Modified umiddelbart (0 ms over nettverk!)
        if (request.checkNotModified(lastModified)) {
            return ResponseEntity.status(304).build()
        }

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic()) // Cache i 30 dager i nettleseren
            .eTag("$lastModified-$length") // Unik ETag basert på filen
            .contentLength(length)
            .body(fileResource)
    }

    @GetMapping("/{deviceId}/thumb/{fileName:.+}")
    fun getThumbMediaStream(
        @PathVariable deviceId: Int,
        @PathVariable fileName: String,
        request: WebRequest
    ): ResponseEntity<Resource> {
        // 1. Hent filen (eventuelt cache serial-oppslaget i et lite minne/map om du vil optimere enda mer)
        val fileResource = photoService.getThumbFile(deviceId, fileName)
        val file = fileResource.file

        // 2. Bruk filens sist endret-tidspunkt og størrelse som et unikt ETag/LastModified-stempel
        val lastModified = file.lastModified()
        val length = file.length()

        // 3. Hvis nettleseren har den i cachen, returner 304 Not Modified umiddelbart (0 ms over nettverk!)
        if (request.checkNotModified(lastModified)) {
            return ResponseEntity.status(304).build()
        }

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic()) // Cache i 30 dager i nettleseren
            .eTag("$lastModified-$length") // Unik ETag basert på filen
            .contentLength(length)
            .body(fileResource)
    }

    @GetMapping("/devices")
    fun getPhotodevices(): List<PhotoDevice> {
        return photoService.getPhotoDevices()
    }


}