package no.iktdev.kammich.system.network

import no.iktdev.kammich.system.SystemCommandService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Component
class GatewayController(private val cmd: SystemCommandService) {

    // Kalles av din overvåker-loop
    fun ensureGatewayRunning() {
        if (!isServiceActive("hostapd")) {
            cmd.runNetworkCommand("systemctl", "start", "hostapd")
        }
        if (!isServiceActive("dnsmasq")) {
            cmd.runNetworkCommand("systemctl", "start", "dnsmasq")
        }
    }

    private fun isServiceActive(service: String): Boolean {
        return try {
            val output = cmd.runNetworkCommand("systemctl", "is-active", service)
            output.trim() == "active"
        } catch (e: Exception) {
            false
        }
    }
}