package no.iktdev.kammich.system.network.wifi.parser

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import no.iktdev.kammich.models.internal.network.IwScanItem
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.utils.WifiUtils
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class WifiScanResultParser(
    private val gson: Gson,
) {

    fun parseJson(json: String): List<IwScanItem> {
        val listType = object : TypeToken<List<IwScanItem>>() {}.type
        return gson.fromJson(json, listType)
    }

    fun iwToFeNetwork(interfaceName: String, items: List<IwScanItem>): List<WifiNetwork> {
        return items.map { raw ->
            val sec = raw.sec()
            val isHidden = raw.ssid.isBlank()
            WifiNetwork(
                ssid = raw.ssid.ifBlank { "[Skjult Nettverk]" },
                signalPercent = WifiUtils.dBmToPercentage(raw.signalDbm.toInt()),
                isSecure = !sec.equals("OPEN", ignoreCase = true),
                bssid = raw.bssid,
                securityType = sec,
                interfaceName = interfaceName,
                isHidden = isHidden,
                channel = raw.channel,
                hwMode = raw.hwMode,
                frequencyMhz = raw.freq ?: 0
            )
        }
    }

    fun IwScanItem.sec(): String {
        val isSecure = this.hasRsn || this.capability.contains("Privacy")

        return when {
            this.authSuites.contains("SAE") -> "WPA3"
            this.hasRsn -> "WPA2"
            isSecure -> "WPA"
            else -> "OPEN"
        }
    }

    fun usingJc(output: String): String? {
        return try {
            val process = ProcessBuilder("jc", "--iw-scan").start()
            process.outputStream.bufferedWriter().use { it.write(output) }

            val jsonOutput = process.inputStream.bufferedReader().use { it.readText() }
            val errorOutput = process.errorStream.bufferedReader().use { it.readText() }

            process.waitFor(5, TimeUnit.SECONDS)

            if (process.exitValue() != 0) {
                throw RuntimeException("jc feilet: ${errorOutput.trim()}")
            } else {
                jsonOutput
            }
        } catch (e: Exception) {
            throw RuntimeException("Kunne ikke kjøre jc: ${e.message}")
        }
    }

}

