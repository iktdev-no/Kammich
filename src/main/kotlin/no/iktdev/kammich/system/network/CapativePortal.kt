package no.iktdev.kammich.system.network

import no.iktdev.kammich.models.shared.network.WifiConnectionResult
import no.iktdev.kammich.models.shared.network.WifiConnectivityState
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URL

@Component
class CapativePortal {

    fun verify(): WifiConnectionResult {
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

                if (responseCode in 300..399) {
                    val portalLocation = connection.getHeaderField("Location") ?: ""
                    return WifiConnectionResult(false, portalLocation, WifiConnectivityState.CAPTIVE_PORTAL)
                } else if (responseCode == 204) {
                    return WifiConnectionResult(true, "Internett-forbindelsen er aktiv.", WifiConnectivityState.CONNECTED)
                }
            } catch (e: Exception) {
                // Prøv neste URL
            }
        }
        return WifiConnectionResult(false, "Ingen nettverksforbindelse detektert.", WifiConnectivityState.DISCONNECTED)
    }
}