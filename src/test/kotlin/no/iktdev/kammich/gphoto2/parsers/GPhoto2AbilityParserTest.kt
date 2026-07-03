package no.iktdev.kammich.gphoto2.parsers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GPhoto2AbilityParserTest {

    private val parser = GPhoto2AbilityParser()

    @Test
    fun `should parse ability data correctly`() {
        val input = """
            Abilities for camera            : Samsung Galaxy models (MTP)
            Serial port support             : no
            USB support                     : yes
            Capture choices                 :
                                            : Image
                                            : Video
            Configuration support           : no
            Delete selected files on camera : yes
            Delete all files on camera      : no
            File preview (thumbnail) support : no
            File upload support             : yes
        """.trimIndent()

        val result = parser.parse(input)

        // Sjekk enkle verdier
        assertThat(result.camera).isEqualTo("Samsung Galaxy models (MTP)")
        assertThat(result.usbSupport).isTrue()
        assertThat(result.serialPortSupport).isFalse()
        assertThat(result.deleteSelectedFiles).isTrue()
        assertThat(result.deleteAllFiles).isFalse()
        assertThat(result.fileUploadSupport).isTrue()

        // Sjekk capture choices (listen)
        assertThat(result.captureChoices).hasSize(2)
        assertThat(result.captureChoices).containsExactly("Image", "Video")
    }

    @Test
    fun `should handle empty or missing capture choices gracefully`() {
        val input = """
            Abilities for camera            : Simple Camera
            Capture choices                 :
                                            : Capture not supported by the driver
            Configuration support           : no
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result.captureChoices).contains("Capture not supported by the driver")
    }

    @Test
    fun `should handle empty or missing capture choices or gibberish gracefully`() {
        val input = """
            Abilities for camera            : Simple Camera
            Capture choices                 :
                                            : Capture not supported by the driver
            Configuration support           : no
            Potet
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result.captureChoices).contains("Capture not supported by the driver")
    }
}