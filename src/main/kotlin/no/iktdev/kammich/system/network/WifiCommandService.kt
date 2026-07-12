package no.iktdev.kammich.system.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import no.iktdev.kammich.models.internal.network.IwScanItem
import no.iktdev.kammich.models.shared.network.ConnectionResult
import no.iktdev.kammich.models.shared.network.ConnectionStatus
import no.iktdev.kammich.models.shared.network.FeWifiNetwork
import no.iktdev.kammich.models.shared.network.WifiInterfaceInfo
import no.iktdev.kammich.models.shared.network.WifiActivityState
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.utils.WifiUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@Service
class WifiCommandService(
    private val gson: Gson,
    private val sseManager: SseManager
) {

    private val log = LoggerFactory.getLogger(WifiCommandService::class.java)

    @Volatile
    private var currentState = WifiActivityState.IDLE

    fun getCurrentState(): WifiActivityState = currentState

    /**
     * Trigger skanning fullstendig asynkront.
     */
    fun triggerScanAsync(interfaceName: String) {
        if (currentState == WifiActivityState.SCANNING) {
            log.info("WiFi-skanning kjører allerede på $interfaceName. Avbryter.")
            return
        }

        thread(start = true, name = "wifi-scan-worker") {
            log.info("Starter asynkron WiFi-skanning på $interfaceName...")
            getNetworks(interfaceName, forceRescan = true)
        }
    }

    /**
     * Kjører skanning, dytter gjennom jc, og caster til frontend-modeller.
     */
    fun getNetworks(interfaceName: String, forceRescan: Boolean): List<FeWifiNetwork> {
        if (forceRescan) {
            currentState = WifiActivityState.SCANNING
            updateSSE(WifiActivityState.SCANNING)
        }

        return try {
            // GREP 1: Vi kjører iw direkte som et array uten shell-pipes for å sikre stabilitet og tidsbruk.
            log.info("Fyrer av 'iw dev $interfaceName scan'...")
            val rawIwOutput = runPureCommand(listOf("sudo", "iw", "dev", interfaceName, "scan"))

            if (rawIwOutput.isBlank()) {
                log.warn("Fikk absolutt ingen tekst tilbake fra iw-skanningen. Sjekk sudo-rettigheter!")
                currentState = WifiActivityState.IDLE
                updateSSE(WifiActivityState.IDLE)
                return emptyList()
            }

            // GREP 2: Nå dytter vi den fangede teksten trygt inn i jc via stdin i stedet for shell-pipes
            val jsonResult = parseWithJc(rawIwOutput)
            if (jsonResult.isBlank()) {
                log.error("Klarte ikke å parse iw-output med jc (kommandolinjeverktøyet jc feilet eller mangler).")
                currentState = WifiActivityState.IDLE
                updateSSE(WifiActivityState.IDLE)
                return emptyList()
            }

            val listType = object : TypeToken<List<IwScanItem>>() {}.type
            val rawNetworks: List<IwScanItem> = gson.fromJson(jsonResult, listType)

            val feNetworks = rawNetworks.map { raw ->
                val isSecure = raw.hasRsn || raw.capability.contains("Privacy")

                val securityType = when {
                    raw.authSuites.contains("SAE") -> "WPA3"
                    raw.hasRsn -> "WPA2"
                    isSecure -> "WPA"
                    else -> "OPEN"
                }

                FeWifiNetwork(
                    ssid = raw.ssid.ifBlank { "[Skjult Nettverk]" },
                    signalPercent = WifiUtils.dBmToPercentage(raw.signalDbm.toInt()),
                    isSecure = isSecure,
                    bssid = raw.bssid,
                    securityType = securityType
                )
            }.sortedByDescending { it.signalPercent }

            currentState = WifiActivityState.IDLE
            updateSSE(WifiActivityState.IDLE, feNetworks)

            feNetworks
        } catch (e: Exception) {
            log.error("Feil under henting eller parsing av WiFi-nettverk", e)
            currentState = WifiActivityState.ERROR
            updateSSE(WifiActivityState.ERROR)
            emptyList()
        }
    }

    /**
     * Kobler til et aksesspunkt ved bruk av nmcli
     */
    fun connectToNetwork(interfaceName: String, ssid: String, password: String?): ConnectionResult {
        currentState = WifiActivityState.CONNECTING
        log.info("Starter oppkobling til SSID: $ssid på grensesnitt: $interfaceName")

        val command = mutableListOf("sudo", "nmcli", "device", "wifi", "connect", ssid, "ifname", interfaceName)
        if (!password.isNullOrBlank()) {
            command.addAll(listOf("password", password))
        }

        return try {
            val process = ProcessBuilder(command).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val errorOutput = process.errorStream.bufferedReader().use { it.readText() }
            val finished = process.waitFor(15, TimeUnit.SECONDS)

            if (!finished) {
                process.destroyForcibly()
                currentState = WifiActivityState.ERROR
                return ConnectionResult(false, "Tilkoblingen timet ut etter 15 sekunder.", ConnectionStatus.FAILED)
            }

            if (process.exitValue() == 0) {
                currentState = WifiActivityState.CONNECTED
                log.info("Vellykket tilkobling til $ssid: $output")
                ConnectionResult(true, "Tilkoblet nettverket $ssid.", ConnectionStatus.CONNECTED)
            } else {
                currentState = WifiActivityState.DISCONNECTED
                log.error("Kunne ikke koble til $ssid. Feilmelding: $errorOutput $output")
                ConnectionResult(false, "Feilet: ${errorOutput.ifBlank { output }}", ConnectionStatus.FAILED)
            }
        } catch (e: Exception) {
            log.error("Kritisk feil under kjøring av nmcli connect", e)
            currentState = WifiActivityState.ERROR
            ConnectionResult(false, "Systemfeil under oppkobling: ${e.message}", ConnectionStatus.FAILED)
        }
    }



    fun ssePayload(state: WifiActivityState, networks: List<FeWifiNetwork> = emptyList()): Map<String, Any> {
        return mapOf(
            "type" to "wifi-update",
            "payload" to mapOf(
                "status" to state,
                "networks" to networks
            )
        )
    }

    fun updateSSE(state: WifiActivityState, networks: List<FeWifiNetwork> = emptyList()) {
        val payload = ssePayload(state, networks)
        log.info("Sender WiFi SSE update: $payload")
        sseManager.send(payload)
    }
}