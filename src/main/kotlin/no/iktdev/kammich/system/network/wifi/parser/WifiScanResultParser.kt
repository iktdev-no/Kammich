package no.iktdev.kammich.system.network.wifi.parser

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import no.iktdev.kammich.models.internal.network.IwScanItem
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.utils.WifiUtils
import org.springframework.stereotype.Component

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
                frequencyMhz = raw.freq
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

}