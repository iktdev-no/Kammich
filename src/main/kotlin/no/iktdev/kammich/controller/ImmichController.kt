package no.iktdev.kammich.controller

import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.immich.ImmichImageService
import no.iktdev.kammich.immich.ImmichService
import no.iktdev.kammich.models.shared.immich.ImmichLoginRequest
import no.iktdev.kammich.models.shared.immich.ImmichUserAccesses
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPostResponseDto
import no.iktdev.kammich.models.shared.immich.api.ImmichUserMe
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/immich")
class ImmichController(
    private val immichService: ImmichService,
    private val imageService: ImmichImageService
) {
    private val log = LoggerFactory.getLogger(javaClass)


    @PostMapping("/login")
    fun userLogin(@RequestBody payload: ImmichLoginRequest): ImmichUserMe {
        return immichService.authenticateAndCreateApiKey(payload)
    }

    @DeleteMapping("/api-keys/{apiKeyId}")
    fun deleteApiKey(@PathVariable apiKeyId: String): ResponseEntity<Void> {
        return immichService.deleteApiKey(apiKeyId)
    }

    @GetMapping("/profile-image")
    fun getProfileImage(@RequestParam userId: UUID): ResponseEntity<ByteArray> {
        val imageBytes = imageService.fetchProfileImageBytes(userId)

        if (imageBytes == null) {
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .body(imageBytes)
    }

    @GetMapping("/access/all")
    fun getAllApiKeys(): List<ImmichUserAccesses> {
        return immichService.getUsersWithAccesses()
    }


}