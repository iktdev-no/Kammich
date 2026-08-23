package no.iktdev.kammich.system.network

import no.iktdev.kammich.models.shared.network.*
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.networking.SSEWifiScanResult
import no.iktdev.kammich.sse.events.networking.SSEWifiScanStatus
import no.iktdev.kammich.system.network.v1.wifi.strategy.scan.WifiScanStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.*

@Service
class WifiScanServiceV2(
    private val sseManager: SseManager,
    private val strategies: List<WifiScanStrategy>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Data-klasse for å holde på både nettverkene og tidsstempel for når cachen ble sist oppdatert
    private data class CachedScan(
        val networks: List<WifiNetwork>,
        val timestamp: Instant = Instant.now()
    )

    // Intern cache med tidsstempel per grensesnitt
    private val scanCache = ConcurrentHashMap<String, CachedScan>()

    // Holder styr på aktive periodiske skanninger per grensesnitt
    private val activeScans = ConcurrentHashMap<String, ScheduledFuture<*>>()
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    companion object {
        private val SCAN_INTERVAL_SECONDS = 5L
        // Hvor lenge cachen anses som "fersk" (f.eks. 10 sekunder)
        private val CACHE_MAX_AGE_SECONDS = 10L
    }

    private fun getActiveStrategy(): WifiScanStrategy? = strategies.find { it.isSupported() }

    fun startPeriodicScan(interfaceName: String) {
        // Hvis det allerede finnes en aktiv skanner, stopp den først for å unngå overlapp
        activeScans.remove(interfaceName)?.let { existingFuture ->
            log.info("Stopper eksisterende periodisk skanning for $interfaceName før oppstart av ny.")
            existingFuture.cancel(true)
        }

        log.info("Starter periodisk skanning for $interfaceName hvert ${SCAN_INTERVAL_SECONDS} sekund")

        val future = scheduler.scheduleAtFixedRate({
            try {
                scan(interfaceName)
            } catch (e: Exception) {
                log.error("Feil under periodisk skanning for $interfaceName", e)
            }
        }, 0, SCAN_INTERVAL_SECONDS, TimeUnit.SECONDS)

        activeScans[interfaceName] = future
    }

    fun stopPeriodicScan(interfaceName: String) {
        activeScans.remove(interfaceName)?.let { future ->
            future.cancel(true)
            log.info("Stoppet periodisk skanning for $interfaceName")
            sseManager.send(SSEWifiScanStatus(WifiScanStatus(interfaceName, isScanning = false)))
        }
    }

    fun getNetworks(interfaceName: String): List<WifiNetwork> {
        val cached = scanCache[interfaceName]
        val now = Instant.now()

        // Sjekk om cachen mangler, er tom, eller er eldre enn tillatt levetid
        val isExpired = cached == null || cached.networks.isEmpty() ||
                cached.timestamp.plusSeconds(CACHE_MAX_AGE_SECONDS).isBefore(now)

        if (isExpired) {
            log.info("Cache for $interfaceName er tom eller utdatert. Utfører umiddelbar skanning...")
            return scan(interfaceName).networks
        }

        return cached.networks
    }

    fun scan(interfaceName: String): WifiScanResult {
        val strategy = getActiveStrategy() ?: run {
            log.error("Ingen støttet wifi-scanner strategi funnet.")
            return WifiScanResult(interfaceName, emptyList(), WifiScanError.Unknown)
        }

        sseManager.send(SSEWifiScanStatus(WifiScanStatus(interfaceName, isScanning = true)))

        return try {
            //log.info("Utfører passiv skanning på $interfaceName uten å endre lease-status")
            val result = strategy.scan(interfaceName)
            val scanResult = WifiScanResult(interfaceName, result.networks)

            // Lagre i cachen med tidsstempel
            scanCache[interfaceName] = CachedScan(result.networks)

            sseManager.send(SSEWifiScanResult(scanResult))
            scanResult
        } catch (e: Exception) {
            log.error("Feil under skanning på $interfaceName", e)
            WifiScanResult(interfaceName, emptyList(), WifiScanError.Unknown)
        } finally {
            sseManager.send(SSEWifiScanStatus(WifiScanStatus(interfaceName, isScanning = false)))
        }
    }
}