package no.iktdev.kammich.system.network.v2

import no.iktdev.kammich.models.shared.network.CaptivePortalState
import no.iktdev.kammich.models.shared.network.NetworkCaptiveStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URL

@Component
class CaptivePortalV2 {
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

                // TODO: Hvis du har bundet socketen/tilkoblingen til et spesifikt interface
                // via NetworkManager/Socket-binding tidligere, kan det gjøres her.

                connection.connect()
                val responseCode = connection.responseCode

                when (responseCode) {
                    in 300..399 -> {
                        val portalLocation = connection.getHeaderField("Location") ?: ""
                        log.info("Captive portal detektert på $interfaceName: $portalLocation")
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
}