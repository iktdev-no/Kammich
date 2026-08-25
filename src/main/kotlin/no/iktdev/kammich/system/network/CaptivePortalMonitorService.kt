package no.iktdev.kammich.system.network

import com.google.gson.Gson
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.net.HttpURLConnection
import java.net.URI

@Service
class CaptivePortalMonitorService(
    private val registryV2: NetworkInterfaceRegistryV2,
    private val captivePortal: CaptivePortal
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val gson = Gson()

    @Scheduled(fixedRate = 30000, initialDelay = 10000)
    fun checkActiveClientInterfaces() {
        val clientInterfaces = registryV2.listNetworkInterfaces().filter {
            it.mode == NetworkInterfaceMode.Client
        }

        if (clientInterfaces.isEmpty()) {
            if (isOverlayOpen()) {
                log.info("Ingen aktive klient-grensesnitt. Lukker åpen captive portal / overlay.")
                closeOverlay()
            }
            return
        }

        for (iface in clientInterfaces) {
            log.debug("Kjører periodisk captive portal-sjekk på grensesnitt: ${iface.interfaceName}")
            try {
                // Produksjonsflyt: La CaptivePortal-klassen håndtere verifiseringen
                captivePortal.verify(iface.interfaceName)
            } catch (e: Exception) {
                log.error("Feil under captive portal-sjekk for ${iface.interfaceName}: ${e.message}")
            }
        }
    }

    private data class OverlayStatusResponse(val active: Boolean)

    private fun isOverlayOpen(): Boolean {
        return try {
            val url = URI.create("http://127.0.0.1:8081/overlay").toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 1000
            conn.readTimeout = 1000

            if (conn.responseCode == 200) {
                val responseString = conn.inputStream.bufferedReader().use { it.readText() }
                val status = gson.fromJson(responseString, OverlayStatusResponse::class.java)
                return status?.active == true
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun closeOverlay() {
        try {
            val url = URI.create("http://127.0.0.1:8081/overlay").toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.responseCode
            log.info("Lukket overlay via pykiosk.")
        } catch (e: Exception) {
            log.error("Klarte ikke å lukke overlay: ${e.message}")
        }
    }
}