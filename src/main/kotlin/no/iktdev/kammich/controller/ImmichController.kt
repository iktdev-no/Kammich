package no.iktdev.kammich.controller

import no.iktdev.kammich.immich.context.ImmichServerContext
import no.iktdev.kammich.immich.exceptions.ImmichException
import no.iktdev.kammich.immich.services.ImmichImageService
import no.iktdev.kammich.immich.services.ImmichService
import no.iktdev.kammich.models.shared.immich.ImmichLoginRequest
import no.iktdev.kammich.models.shared.immich.ImmichUserAccesses
import no.iktdev.kammich.models.shared.immich.api.ImmichServerConfig
import no.iktdev.kammich.models.shared.immich.api.ImmichServerConnection
import no.iktdev.kammich.models.shared.immich.api.ImmichServerFeatures
import no.iktdev.kammich.models.shared.immich.api.ImmichServerStorage
import no.iktdev.kammich.models.shared.immich.api.ImmichServerVersion
import no.iktdev.kammich.models.shared.immich.api.ImmichSupportedMediaTypes
import no.iktdev.kammich.models.shared.immich.api.ImmichUserMe
import org.slf4j.LoggerFactory
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
    private val imageService: ImmichImageService,
    private val immichServerContext: ImmichServerContext,
) {
    private val log = LoggerFactory.getLogger(javaClass)


    @PostMapping("/login")
    fun userLogin(@RequestBody payload: ImmichLoginRequest): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(immichService.login(payload))
        }
        catch (e: ImmichException) {
            ResponseEntity.badRequest().body(e.message ?: "En feil oppstod under innlogging")
        }
    }

    @GetMapping("/logout")
    fun userLogout() {
        return immichService.logout()
    }

    @PostMapping("/change/user/{userId}")
    fun changeUser(@PathVariable userId: UUID): Boolean {
        return immichService.switchUser(userId)
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

    @GetMapping("/server/url")
    fun getServerUrl(): ResponseEntity<Any> {
        val url = immichServerContext.getServerUrl()
        return if (url == null) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(ImmichServerConnection(url))
        }
    }

    @GetMapping("/server/version")
    fun getServerVersion(): ResponseEntity<ImmichServerVersion> {
        return ResponseEntity.ok(immichService.getServerVersion())
    }

    @GetMapping("/server/supported-media-types")
    fun getSupportedMediaTypes(): ResponseEntity<ImmichSupportedMediaTypes> {
        return ResponseEntity.ok(immichService.getServerSupportedMediaTypes())
    }

    @GetMapping("/server/features")
    fun getSupportedFeatures(): ResponseEntity<ImmichServerFeatures> {
        return ResponseEntity.ok(immichService.getServerFeatures())
    }

    @GetMapping("/server/config")
    fun getConfigs(): ResponseEntity<ImmichServerConfig> {
        return ResponseEntity.ok(immichService.getServerConfig())
    }

    @GetMapping("/server/storage")
    fun getStorage(): ResponseEntity<ImmichServerStorage> {
        return ResponseEntity.ok(immichService.getServerStorage())
    }

    @GetMapping("/access/all")
    fun getAllApiKeys(): List<ImmichUserAccesses> {
        return immichService.getUsersWithAccesses()
    }

    @GetMapping("/users")
    fun getUsers(): List<ImmichUserMe> {
        return immichService.getUsers()
    }


}