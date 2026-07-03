package no.iktdev.kammich.gphoto2.parsers

import no.iktdev.kammich.gphoto2.model.GPhoto2NodeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GPhoto2FileListParserTest {

    private val parser = GPhoto2FileListParser()

    @Test
    @DisplayName("Skal parse rot-mappe og filer med 'rd' attributt korrekt")
    fun `should parse non-recursive folder and file listing`() {
        val input = """
        There are 23 folders in folder '/store_00010001'.
         - ASD
         - Alarms
        There is 1 file in folder '/store_00010001'.
        #1     QTAudioEngine            rd     1 KB application/x-unknown 1659796433
    """.trimIndent()

        val result = parser.parse(input)

        // Verifiser mappe
        val asdFolder = result.find { it.name == "ASD" }
        assertThat(asdFolder?.folderPath).isEqualTo("/store_00010001/ASD")

        // Verifiser fil
        val file = result.find { it.name == "QTAudioEngine" }
        assertThat(file).isNotNull
        assertThat(file?.sizeBytes).isEqualTo(1024) // 1 KB
        assertThat(file?.mimeType).isEqualTo("application/x-unknown")
    }

    @Test
    @DisplayName("Skal parse komplekse filnavn og beholde korrekt path")
    fun `should handle spaces in names and correct paths`() {
        val input = """
            There are 1 files in folder '/store_0001/Music'.
            #1    My Cool Song (Remix 2026).mp3    4096 KB audio/mpeg 1629200643
        """.trimIndent()

        val result = parser.parse(input)
        val musicFile = result.find { it.name == "My Cool Song (Remix 2026).mp3" }

        assertThat(musicFile).isNotNull
        assertThat(musicFile?.folderPath).isEqualTo("/store_0001/Music")
    }

    @Test
    @DisplayName("Skal ignorere tomme mapper og håndtere 0 filer")
    fun `should handle empty folder list gracefully`() {
        val input = "There are 0 files in folder '/empty'."
        val result = parser.parse(input)
        assertThat(result.filter { it.type == GPhoto2NodeType.FILE }).isEmpty()
    }

    @Test
    @DisplayName("Skal parse DCIM-filer med 'rd' attributt og korrekt filstørrelse")
    fun `should parse DCIM camera files correctly`() {
        val input = """
            There are 0 folders in folder '/store_00010001/DCIM/Camera'.
            There are 5 files in folder '/store_00010001/DCIM/Camera'.
            #1     20201116_165231.jpg        rd  3234 KB image/jpeg 1605545551
            #2     20210913_101331.jpg        rd  3180 KB image/jpeg 1631528011
            #3     20220330_223704.jpg        rd  5386 KB image/jpeg 1648679824
            #4     20220409_234535.jpg        rd  5273 KB image/jpeg 1649547935
            #5     20220413_125949.jpg        rd  6446 KB image/jpeg 1649854789
        """.trimIndent()

        val result = parser.parse(input)
        val files = result.filter { it.type == GPhoto2NodeType.FILE }

        assertThat(files).hasSize(5)

        // Verifiser første fil
        val firstFile = files.find { it.name == "20201116_165231.jpg" }
        assertThat(firstFile).isNotNull
        assertThat(firstFile?.sizeBytes).isEqualTo(3234 * 1024)
        assertThat(firstFile?.folderPath).isEqualTo("/store_00010001/DCIM/Camera")

        // Verifiser siste fil
        val lastFile = files.find { it.name == "20220413_125949.jpg" }
        assertThat(lastFile?.sizeBytes).isEqualTo(6446 * 1024)
    }

}