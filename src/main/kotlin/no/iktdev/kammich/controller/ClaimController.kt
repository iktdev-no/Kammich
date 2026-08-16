package no.iktdev.kammich.controller


import no.iktdev.kammich.immich.context.ImmichUserContext
import no.iktdev.kammich.models.shared.device.DeviceOwnershipSummary
import no.iktdev.kammich.models.shared.device.ImportJobOwnershipSummary
import no.iktdev.kammich.services.ClaimOwnershipService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/v1/claim")
class ClaimController(
    private val immichUserContext: ImmichUserContext,
    private val claimOwnershipService: ClaimOwnershipService
) {

    @GetMapping("/device")
    fun getDevices(): List<DeviceOwnershipSummary> {
        return claimOwnershipService.getDevices()
    }

    @GetMapping("/import-job")
    fun getImportJobs(): List<ImportJobOwnershipSummary> {
        return claimOwnershipService.getImportJobs()
    }


    @PostMapping("/device/{deviceSN}")
    fun claimDevice(@PathVariable deviceSN: String): ResponseEntity<out Map<String, Any>> {
        val userId = immichUserContext.getCurrentUserId()
            ?: return ResponseEntity.status(401).body(mapOf("error" to "Ingen aktiv Immich-bruker"))

        val success = claimOwnershipService
            .claimDevice(userId, deviceSN)

        return if (success) {
            ResponseEntity.ok(mapOf("success" to true, "message" to "Enhet $deviceSN er nå knyttet til bruker"))
        } else {
            ResponseEntity.status(500).body(mapOf("error" to "Klarte ikke å claime enhet"))

        }
    }

    @PostMapping("/import-job/{importJobId}")
    fun claimImportJob(@PathVariable importJobId: UUID): ResponseEntity<out Map<String, Any>> {
        val userId = immichUserContext.getCurrentUserId()
            ?: return ResponseEntity.status(401).body(mapOf("error" to "Ingen aktiv Immich-bruker"))

        val success = claimOwnershipService.claimImportJob(importJobId, userId)
        return if (success) {
            ResponseEntity.ok(mapOf("success" to true, "message" to "Import-jobb $importJobId er nå knyttet til bruker"))
        } else {
            ResponseEntity.status(500).body(mapOf("error" to "Klarte ikke å claime import-jobb"))

        }
    }
}