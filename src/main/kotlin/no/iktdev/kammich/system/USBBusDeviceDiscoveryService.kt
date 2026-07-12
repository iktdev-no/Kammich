package no.iktdev.kammich.system

import no.iktdev.kammich.models.internal.SysPathReady
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.io.File

@Service
class USBBusDeviceDiscoveryService(
    private val eventPublisher: ApplicationEventPublisher,
) {

    private val log = LoggerFactory.getLogger(USBBusDeviceDiscoveryService::class.java)


    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        log.info("Application ready, proceeding to discover present devices")
        val usbDir = File("/sys/bus/usb/devices")
        usbDir.listFiles { _, name -> name.matches(Regex("\\d+-\\d+(\\.\\d+)*")) }?.forEach { deviceDir ->
            // Sjekk om dette er en hovedenhet (uten kolon i navnet)
            if (!deviceDir.name.contains(":")) {
                val sysPath = deviceDir.canonicalPath
                eventPublisher.publishEvent(SysPathReady(sysPath))

            }
        }
    }
}