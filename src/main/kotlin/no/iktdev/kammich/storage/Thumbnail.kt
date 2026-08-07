package no.iktdev.kammich.storage

import net.coobird.thumbnailator.Thumbnails
import org.springframework.core.io.FileSystemResource
import java.io.File
import java.io.FileNotFoundException

class Thumbnail(val store: File) {
    init {
        if (!store.exists()) {
            store.mkdirs()
        }
    }

    fun getThumbnailOf(file: File): FileSystemResource {

        val thumbnail = File(store, file.name)
        if (thumbnail.exists() && thumbnail.isFile) {
            return FileSystemResource(thumbnail)
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


        return FileSystemResource(thumbnail)
    }
}