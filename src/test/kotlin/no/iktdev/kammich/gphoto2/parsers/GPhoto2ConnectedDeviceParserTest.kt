package no.iktdev.kammich.gphoto2.parsers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GPhoto2ConnectedDeviceParserTest {
    private val parser = GPhoto2ConnectedDeviceParser()

    @Test
    fun `should parse multiple connected devices correctly`() {
        val input = """
            Model                          Port                                            
            ----------------------------------------------------------
            Samsung Galaxy models (MTP)    usb:001,032     
            Samsung Galaxy models (MTP)    usb:001,033     
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result).hasSize(2)
        assertThat(result[0].model).isEqualTo("Samsung Galaxy models (MTP)")
        assertThat(result[0].port).isEqualTo("usb:001,032")
        assertThat(result[1].port).isEqualTo("usb:001,033")
    }

    @Test
    fun `should handle empty input gracefully`() {
        val result = parser.parse("")
        assertThat(result).isEmpty()
    }


    @Test
    fun `should handle no devices`() {
        val input = """
            Model                          Port                                            
            ----------------------------------------------------------
        """.trimIndent()

        val result = parser.parse(input)
        assertThat(result).isEmpty()
    }

    @Test
    fun `should parse multiple connected devices correctly and ignore random gibberish`() {
        val input = """
            Model                          Port                                            
            ----------------------------------------------------------
            Samsung Galaxy models (MTP)    usb:001,032     
            Samsung Galaxy models (MTP)    usb:001,033     
            Samsung Galaxy models (MTP)    potet:001,034     
            Potetmos
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result).hasSize(2)
        assertThat(result[0].model).isEqualTo("Samsung Galaxy models (MTP)")
        assertThat(result[0].port).isEqualTo("usb:001,032")
        assertThat(result[1].port).isEqualTo("usb:001,033")
    }
}