package no.iktdev.kammich.gphoto2

import no.iktdev.kammich.gphoto2.model.GPhoto2File
import no.iktdev.kammich.gphoto2.model.GPhoto2NodeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class GPhoto2CommandBuilderTest {

    @Test
    fun `should build correct copy command with all flags`() {
        val builder = GPhoto2CommandBuilder()
        val file = GPhoto2File(
            name = "bilde.jpg",
            type = GPhoto2NodeType.FILE,
            folderPath = "/store_0001/DCIM",
            sizeBytes = 1024,
            mimeType = "image/jpeg"
        )
        val destination = File("/home/user/downloads")

        builder.port("usb:001,036").copy(destination, file.folderPath, file.name)
        val args = builder.build()

        // Sjekk at alle nødvendige flagg er med i korrekt rekkefølge
        assertThat(args).containsExactly(
            "--port", "usb:001,036",
            "--folder", "/store_0001/DCIM",
            "--get-file", "bilde.jpg",
            "--filename", "/home/user/downloads/bilde.jpg"
        )
    }

    @Test
    fun `should build correct delete command`() {
        val builder = GPhoto2CommandBuilder()
        val file = GPhoto2File(
            name = "test.jpg",
            type = GPhoto2NodeType.FILE,
            folderPath = "/store_0001/DCIM",
            sizeBytes = 1024,
            mimeType = "image/jpeg"
        )

        builder.port("usb:001,036").delete(file)
        val args = builder.build()

        assertThat(args).containsExactly(
            "--port", "usb:001,036",
            "--folder", "/store_0001/DCIM",
            "--delete-file", "test.jpg"
        )
    }
}