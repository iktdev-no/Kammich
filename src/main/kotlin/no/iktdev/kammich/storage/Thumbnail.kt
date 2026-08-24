package no.iktdev.kammich.storage

import net.coobird.thumbnailator.Thumbnails
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import java.io.File

class Thumbnail(val store: File) {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        if (!store.exists()) {
            store.mkdirs()
        }
    }

    /**
     * Genererer thumbnail preventivt på forhånd dersom den ikke finnes fra før.
     */
    fun generate(file: File): File {
        val thumbnail = File(store, file.name)
        if (thumbnail.exists() && thumbnail.isFile) {
            return thumbnail
        } else {
            log.info("Generating thumbnail for file ${file.name}")
        }

        // Thumbnailator leser og skalerer direkte fra filstrømmen,
        // noe som forhindrer OutOfMemoryError på 20MB+ bilder,
        // og .useExifOrientation(true) retter opp roterte mobilbilder automatisk.
        Thumbnails.of(file)
            .width(400)
            .keepAspectRatio(true)
            .useExifOrientation(true)
            .outputFormat("jpg")
            .outputQuality(0.80)
            .toFile(thumbnail)

        return thumbnail
    }

    fun getThumbnailOf(file: File): FileSystemResource {
        val thumbnail = generate(file)
        return FileSystemResource(thumbnail)
    }
}