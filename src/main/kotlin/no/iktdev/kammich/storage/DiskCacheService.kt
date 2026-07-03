package no.iktdev.kammich.storage

import no.iktdev.kammich.ConfigService
import org.springframework.stereotype.Service
import java.io.File

@Service
class DiskCacheService(private val config: ConfigService) {

    fun getCacheDirectory(): File {
        val dir = File(config.getConfig().cachePath)

        // Sjekk at vi har tilgang
        if (!dir.exists() && !dir.mkdirs()) {
            throw IllegalStateException("Kunne ikke opprette cache-mappe på ${dir.absolutePath}")
        }
        return dir
    }

    // Praktisk når en batch er ferdig lastet opp
    fun deleteFromCache(fileName: String) {
        val file = File(getCacheDirectory(), fileName)
        if (file.exists()) {
            file.delete()
        }
    }
}