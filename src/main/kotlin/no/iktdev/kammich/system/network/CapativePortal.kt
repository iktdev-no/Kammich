package no.iktdev.kammich.system.network

import no.iktdev.kammich.models.shared.network.CaptivePortalState
import no.iktdev.kammich.models.shared.network.NetworkCaptiveStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter

@Component
class CaptivePortal {
    private val log = LoggerFactory.getLogger(javaClass)

    fun verify(interfaceName: String): NetworkCaptiveStatus {
        val captiveUrls = listOf(
            "http://connectivitycheck.gstatic.com/generate_204",
            "http://www.msftconnecttest.com/connecttest.txt"
        )

        for (urlStr in captiveUrls) {
            try {
                val url = URL(urlStr)
                val connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.useCaches = false

                connection.connect()
                val responseCode = connection.responseCode

                when (responseCode) {
                    in 300..399 -> {
                        val portalLocation = connection.getHeaderField("Location") ?: ""
                        log.info("Captive portal detektert på $interfaceName: $portalLocation")

                        // Send URL-en videre til Python-kiosken via FastAPI-endepunktet på port 8081
                        triggerKioskOverlay(portalLocation)

                        return NetworkCaptiveStatus(
                            interfaceName = interfaceName,
                            state = CaptivePortalState.CaptivePortal,
                            portalUrl = portalLocation,
                            message = "Omdirigert til captive portal"
                        )
                    }
                    204 -> {
                        return NetworkCaptiveStatus(
                            interfaceName = interfaceName,
                            state = CaptivePortalState.Online,
                            message = "Internett-forbindelsen er aktiv."
                        )
                    }
                }
            } catch (e: Exception) {
                // Prøv neste URL
            }
        }

        return NetworkCaptiveStatus(
            interfaceName = interfaceName,
            state = CaptivePortalState.Offline,
            message = "Ingen nettverksforbindelse detektert."
        )
    }

    private fun triggerKioskOverlay(portalUrl: String) {
        try {
            // Vi bruker URI.create().toURL() for å unngå deprecation-advarsler
            val fullUrl = java.net.URI.create("http://127.0.0.1:8081/overlay?url=${java.net.URLEncoder.encode(portalUrl, "UTF-8")}").toURL()
            val postConn = fullUrl.openConnection() as HttpURLConnection
            postConn.requestMethod = "POST"
            postConn.connectTimeout = 2000
            postConn.readTimeout = 2000
            postConn.responseCode // Trigg forespørselen

            log.info("Sendte captive portal URL til pykiosk overlay: $portalUrl")
        } catch (e: Exception) {
            log.error("Klarte ikke å kontakte local pykiosk for å åpne overlay: ${e.message}")
        }
    }
}