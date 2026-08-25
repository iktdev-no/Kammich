package no.iktdev.kammich.gphoto2.parsers

import no.iktdev.kammich.gphoto2.model.GPhoto2NodeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GPhoto2FileListParserTest {

    private val parser = GPhoto2FileListParser()

    @Test
    @DisplayName("Skal parse mappe fra absolutt sti")
    fun `should parse folder from absolute path`() {
        val input = """
            /store_00010001
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result).hasSize(1)

        val folder = result.single()
        assertThat(folder.name).isEqualTo("store_00010001")
        assertThat(folder.type).isEqualTo(GPhoto2NodeType.FOLDER)
        assertThat(folder.folderPath).isEqualTo("/store_00010001")
    }

    @Test
    @DisplayName("Skal parse fil med FILENAME, FILESIZE og FILETYPE")
    fun `should parse file correctly`() {
        val input = """
            FILENAME='/store_00010001/QTAudioEngine' FILESIZE=1024 FILETYPE=application/x-unknown
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result).hasSize(1)

        val file = result.single()
        assertThat(file.name).isEqualTo("QTAudioEngine")
        assertThat(file.type).isEqualTo(GPhoto2NodeType.FILE)
        assertThat(file.folderPath).isEqualTo("/store_00010001")
        assertThat(file.sizeBytes).isEqualTo(1024)
    }

    @Test
    @DisplayName("Skal beholde filnavn med mellomrom og parenteser")
    fun `should handle spaces and special characters in file names`() {
        val input = """
            FILENAME='/store_0001/Music/My Cool Song (Remix 2026).mp3' FILESIZE=4096 FILETYPE=audio/mpeg
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result).hasSize(1)

        val file = result.single()
        assertThat(file.name).isEqualTo("My Cool Song (Remix 2026).mp3")
        assertThat(file.folderPath).isEqualTo("/store_0001/Music")
        assertThat(file.sizeBytes).isEqualTo(4096)
    }

    @Test
    @DisplayName("Skal håndtere tom input")
    fun `should handle empty input`() {
        val result = parser.parse("")

        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("Skal ignorere tomme linjer")
    fun `should ignore empty lines`() {
        val input = """
            
            /store_00010001
            
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result).hasSize(1)
        assertThat(result.single().name).isEqualTo("store_00010001")
    }

    @Test
    @DisplayName("Skal parse flere kamerafiler korrekt")
    fun `should parse multiple camera files`() {
        val input = """
            FILENAME='/store_00010001/DCIM/Camera/20201116_165231.jpg' FILESIZE=3311616 FILETYPE=image/jpeg
            FILENAME='/store_00010001/DCIM/Camera/20210913_101331.jpg' FILESIZE=3256320 FILETYPE=image/jpeg
            FILENAME='/store_00010001/DCIM/Camera/20220330_223704.jpg' FILESIZE=5515264 FILETYPE=image/jpeg
            FILENAME='/store_00010001/DCIM/Camera/20220409_234535.jpg' FILESIZE=5399552 FILETYPE=image/jpeg
            FILENAME='/store_00010001/DCIM/Camera/20220413_125949.jpg' FILESIZE=6600704 FILETYPE=image/jpeg
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result).hasSize(5)
        assertThat(result).allMatch { it.type == GPhoto2NodeType.FILE }
        assertThat(result).allMatch {
            it.folderPath == "/store_00010001/DCIM/Camera"
        }
    }

    @Test
    @DisplayName("Skal beholde korrekt JPG-endelse når MIME-type er image/jpeg")
    fun `should preserve valid jpg extension`() {
        val input = """
            FILENAME='/DCIM/Camera/photo.JPG' FILESIZE=1234 FILETYPE=image/jpeg
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result).hasSize(1)
        assertThat(result.single().name).isEqualTo("photo.JPG")
    }

    @Test
    @DisplayName("Skal legge til JPG-endelse når image/jpeg mangler gyldig endelse")
    fun `should enforce jpg extension for jpeg mime type`() {
        val input = """
            FILENAME='/DCIM/Camera/photo' FILESIZE=1234 FILETYPE=image/jpeg
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result).hasSize(1)
        assertThat(result.single().name).isEqualTo("photo.jpg")
    }

    @Test
    @DisplayName("Skal legge til PNG-endelse når image/png mangler gyldig endelse")
    fun `should enforce png extension for png mime type`() {
        val input = """
            FILENAME='/DCIM/Camera/image' FILESIZE=5678 FILETYPE=image/png
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result).hasSize(1)
        assertThat(result.single().name).isEqualTo("image.png")
    }

    @Test
    @DisplayName("Skal ikke endre ukjent MIME-type")
    fun `should not modify filename for unknown mime type`() {
        val input = """
            FILENAME='/store_0001/file.unknown' FILESIZE=100 FILETYPE=application/octet-stream
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result).hasSize(1)
        assertThat(result.single().name).isEqualTo("file.unknown")
    }

    @Test
    @DisplayName("Skal fjerne duplikater basert på path og filnavn")
    fun `should remove duplicate files`() {
        val input = """
            FILENAME='/DCIM/photo.jpg' FILESIZE=1234 FILETYPE=image/jpeg
            FILENAME='/DCIM/photo.jpg' FILESIZE=1234 FILETYPE=image/jpeg
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result).hasSize(1)
    }

    @Test
    @DisplayName("Skal bevare filstørrelse korrekt")
    fun `should parse file size correctly`() {
        val input = """
            FILENAME='/DCIM/large.jpg' FILESIZE=123456789 FILETYPE=image/jpeg
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result.single().sizeBytes).isEqualTo(123456789L)
    }
}