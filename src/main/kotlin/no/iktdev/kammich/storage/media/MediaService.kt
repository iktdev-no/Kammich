package no.iktdev.kammich.storage.media

import no.iktdev.kammich.models.shared.RemoteFile
import org.springframework.core.io.FileSystemResource

interface MediaService {
    fun getFile(deviceId: Int, filename: String): FileSystemResource
    fun getPagedFiles(deviceId: Int, page: Int, size: Int): List<RemoteFile>
    fun getPagedFiles(page: Int, size: Int): List<RemoteFile>
    fun getTotalCount(): Long
}