package no.iktdev.kammich.system.network

import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.net.InetSocketAddress
import java.net.Socket

@Component
class InternetChecker {

    fun isInternetReachable(): Boolean {
        return try {
            // Vi bruker en kort timeout (3 sekunder)
            // 1.1.1.1 er Cloudflare, 53 er DNS-porten
            Socket().use { socket ->
                socket.connect(InetSocketAddress("1.1.1.1", 53), 3000)
                true
            }
        } catch (e: Exception) {
            // Logg feilen hvis du vil debugge, men for logikken er "false" nok
            false
        }
    }
}