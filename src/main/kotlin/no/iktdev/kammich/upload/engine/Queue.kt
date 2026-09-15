package no.iktdev.kammich.upload.engine

import no.iktdev.kammich.upload.model.UploadJob
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

@Component
class Queue {
    private val jobs = ConcurrentLinkedQueue<UploadJob>()

    fun submit(job: UploadJob) {
        jobs.add(job)
    }

    fun next(): UploadJob? =
        jobs.poll()

    fun remove(job: UploadJob): Boolean =
        jobs.remove(job)

    fun clear() {
        jobs.clear()
    }

    fun isEmpty(): Boolean =
        jobs.isEmpty()

    fun removeById(jobId: UUID): Boolean =
        jobs.removeIf { it.jobId == jobId }

    fun getJobs() = jobs.toList()
}