package no.iktdev.kammich.controller

import no.iktdev.kammich.models.shared.upload.UploadJobSummary
import no.iktdev.kammich.models.shared.upload.UploadSummary
import no.iktdev.kammich.services.UploadService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/upload")
class UploadController(
    private val uploadService: UploadService,
) {

    @GetMapping("/user/{userId}")
    fun checkUploadQueue(@PathVariable userId: UUID) {
        uploadService.getCheckForRemainingFiles(userId)
    }

    @PostMapping("/user/{userId}/reset")
    fun resetUploadQueueByUserId(@PathVariable userId: UUID) {
        uploadService.resetFailedUploadsByUser(userId)
    }

    @PostMapping("/user/{userId}/reset/{jobId}")
    fun resetUploadQueueByJobId(@PathVariable userId: UUID, @PathVariable jobId: UUID) {
        uploadService.resetFailedUploadJob(userId, jobId)
    }

    @GetMapping("/user/{userId}/stats")
    fun getUploadStats(@PathVariable userId: UUID): UploadSummary {
        return uploadService.getUploadSummary(userId)
    }

    @GetMapping("/user/{userId}/jobs")
    fun getUploadJobs(@PathVariable userId: UUID): List<UploadJobSummary> {
        return uploadService.getJobUploadSummaries(userId)
    }

    @PostMapping("/user/{userId}/upload/{fileId}")
    fun uploadFile(@PathVariable userId: UUID, @PathVariable fileId: Long) {
        uploadService.uploadFile(userId, fileId)
    }

}