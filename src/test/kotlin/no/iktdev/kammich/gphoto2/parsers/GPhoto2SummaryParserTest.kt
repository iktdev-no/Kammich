package no.iktdev.kammich.gphoto2.parsers

import org.junit.jupiter.api.Assertions.*

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GPhoto2SummaryParserTest {

    private val parser = GPhoto2SummaryParser()

    @Test
    fun `should parse samsung summary correctly`() {
        val input = """
            Manufacturer: Samsung Electronics Co., Ltd.
            Model: SM-G955F
            Serial Number: R58J56T1PAA
            Battery Level (5001 ro u8 ): Range [0 - 100, step 10] value: 100% (100)
            
            Storage Devices Summary:
            store_00010001:
             StorageDescription: Internal
             Maximum Capability: 64000000000 bytes
             Free Space (Bytes): 12000000000 bytes
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result.manufacturer).isEqualTo("Samsung Electronics Co., Ltd.")
        assertThat(result.model).isEqualTo("SM-G955F")
        assertThat(result.serialNumber).isEqualTo("R58J56T1PAA")
        assertThat(result.batteryLevel).isEqualTo(100)

        assertThat(result.storageDevices).hasSize(1)
        assertThat(result.storageDevices[0].id).isEqualTo("store_00010001")
        assertThat(result.storageDevices[0].capacityBytes).isEqualTo(64000000000L)
        assertThat(result.storageDevices[0].freeSpaceBytes).isEqualTo(12000000000L)
    }

    @Test
    fun `should handle missing storage or battery values gracefully`() {
        // En "skitten" input for å teste defensivitet
        val input = """
            Model: Minimalist Camera
            Battery Level: No data
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result.model).isEqualTo("Minimalist Camera")
        // Skal returnere 0 i stedet for å krasje appen
        assertThat(result.batteryLevel).isEqualTo(0)
        assertThat(result.storageDevices).isEmpty()
    }

    @Test
    fun `should survive chaotic input and multiple storage devices`() {
        val input = """
        Manufacturer: Nikon Corp.
        Model: D850
        // No serial number here, should be null/empty
        Battery Level (5001 ro u8 ): value: 42% (42)
        Friendly Device Name (d402 rw str): My Awesome Nikon ('NikonD850')
        
        Storage Devices Summary:
        store_00010001:
         StorageDescription: SD Card
         Maximum Capability: 128000000000 bytes
         Free Space (Bytes): 64000000000 bytes
        store_00020001:
         StorageDescription: XQD Card
         Maximum Capability: 64000000000 bytes
         Free Space (Bytes): 1000000000 bytes
         
        Device Property Summary:
        [Unknown Property] (0000 -- ---): PTP erroaz<r 201b on query
    """.trimIndent()

        val result = parser.parse(input)

        // Sjekk at vi takler manglende serial (nullable)
        assertThat(result.serialNumber).isNullOrEmpty()

        // Sjekk friendly name
        assertThat(result.friendlyDeviceName).isEqualTo("My Awesome Nikon")

        // Sjekk at vi takler multiple storage devices
        assertThat(result.storageDevices).hasSize(2)
        assertThat(result.storageDevices[0].id).isEqualTo("store_00010001")
        assertThat(result.storageDevices[1].description).isEqualTo("XQD Card")
        assertThat(result.storageDevices[1].freeSpaceBytes).isEqualTo(1000000000L)
    }
}