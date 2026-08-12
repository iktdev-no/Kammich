package no.iktdev.kammich.controller

import no.iktdev.kammich.models.shared.Album
import no.iktdev.kammich.models.shared.AlbumCreateRequest
import no.iktdev.kammich.models.shared.AlbumUpdateRequest
import no.iktdev.kammich.services.AlbumService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/album")
class AlbumController(
    private val albumService: AlbumService
) {

    @GetMapping
    fun getAllAlbums(): ResponseEntity<List<Album>> {
        return ResponseEntity.ok(albumService.getAlbums())
    }

    @PostMapping
    fun createAlbum(@RequestBody request: AlbumCreateRequest): ResponseEntity<Long> {
        val id = albumService.createAlbum(request)
        return ResponseEntity.ok(id)
    }

    @PutMapping("/{id}")
    fun updateAlbum(
        @PathVariable id: Long,
        @RequestBody request: AlbumUpdateRequest
    ): ResponseEntity<Void> {
        val success = albumService.updateAlbum(id, request)
        return if (success) ResponseEntity.ok().build() else ResponseEntity.notFound().build()
    }

    @DeleteMapping("/{id}")
    fun deleteAlbum(@PathVariable id: Long): ResponseEntity<Void> {
        val success = albumService.deleteAlbum(id)
        return if (success) ResponseEntity.ok().build() else ResponseEntity.notFound().build()
    }
}