package no.iktdev.kammich.system

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.springframework.context.ApplicationEventPublisher
import kotlin.test.Test

class UdevServiceTest {
    val eventPublisher = mockk<ApplicationEventPublisher>()

    @Test
    fun `skal parse korrekt add event for hovedenhet`() {
        val service = UdevService(eventPublisher)
        val line = "KERNEL[35934.080102] add      /devices/pci0000:00/0000:00:14.0/usb1/1-11 (usb)"
        val result = service.parseUdevEvent(line)

        assert(result?.event == "add")
        assert(result?.path == "/devices/pci0000:00/0000:00:14.0/usb1/1-11")
    }

    @Test
    fun `skal ignorere interfaces med kolon`() {
        val service = UdevService(eventPublisher)
        val line = "KERNEL[12345] bind     /devices/pci0000:00/usb1/1-11/1-11:1.0"
        val result = service.parseUdevEvent(line)

        assert(result == null) // Bør returnere null da det er en sub-enhet
    }
}