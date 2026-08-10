package no.iktdev.kammich.controller

import no.iktdev.kammich.services.NotificationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notification")
class NotificationController(
    private val notificationService: NotificationService // Antatt at du har en service for dette
) {

    @DeleteMapping("/dismiss-all")
    fun dismissAll(): ResponseEntity<Void> {
        notificationService.dismissAll()
        return ResponseEntity.noContent().build() // 204 No Content
    }

    @DeleteMapping("/{id}")
    fun dismiss(@PathVariable id: String): ResponseEntity<Void> {
        val success = notificationService.dismiss(id)
        return if (success) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}