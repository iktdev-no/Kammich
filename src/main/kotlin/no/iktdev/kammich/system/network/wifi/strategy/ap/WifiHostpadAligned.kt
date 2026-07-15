package no.iktdev.kammich.system.network.wifi.strategy.ap

import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiSecurityType
import no.iktdev.kammich.models.shared.network.WifiTetherSetting
import no.iktdev.kammich.models.shared.network.WifiTetheringNetwork
import no.iktdev.kammich.system.network.wifi.WifiRunner
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File

@Component
class WifiHostpadAligned(
    private val runner: WifiRunner,
    private val configService: ConfigService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun isRunning(): Boolean {
        return runner.run("pgrep", "-f", "hostapd") is WifiRunner.CommandResult.Success
    }

    fun startAligned(interfaceName: String, tether: WifiTetherSetting, network: WifiNetwork): WifiTetheringNetwork {
        logger.info("Aligning AP to SSID: ${network.ssid} on channel ${network.channel}")
        val confPath = configService.getConfig().kammichHostpadPath

        val targetFreq = network.frequencyMhz ?: 5280
        val targetChannel = network.channel ?: 56

        val wpaConfig = when (tether.security) {
            WifiSecurityType.NONE -> "wpa=0"
            WifiSecurityType.WPA2 -> "wpa=2\nwpa_key_mgmt=WPA-PSK\nrsn_pairwise=CCMP"
            WifiSecurityType.WPA3 -> "wpa=2\nieee80211w=2\nwpa_key_mgmt=WPA-PSK-SHA256\nrsn_pairwise=GCMP"
        }

        // 2. Generer konfigurasjon 1:1
        val config = """
            # aligned_to=${network.ssid}
            interface=$interfaceName
            driver=nl80211
            ssid=${tether.ssid}
            hw_mode=${network.hwMode ?: "a"}
            channel=$targetChannel
            frequency=$targetFreq
            
            # WPA2/3 sikkerhet
            $wpaConfig
            wpa_passphrase=${tether.password}
            
            # Stabilitets-flagg
            ieee80211n=1
            ieee80211ac=1
            wmm_enabled=1
            auth_algs=1
        """.trimIndent()

        // 3. Skriv til tmpfs
        val configFile = File(confPath)
        configFile.writeText(config)

        // 4. Start hostapd som demon (-B)
        val result = runner.run("hostapd", "-B", confPath)
        if (result !is WifiRunner.CommandResult.Success) {
            throw IllegalStateException("Failed to start hostapd")
        }

        return WifiTetheringNetwork(
            ssid = tether.ssid,
            channel = targetChannel,
            frequencyMhz = targetFreq,
            alignedToSSID = network.ssid,
            isAligned = true
        )
    }

    fun stopAligned(): Boolean {
        if (!isRunning()) {
            logger.info("AP Is already stopped")
            return true
        }
        logger.info("Stopping AP")
        val result = runner.run("sudo", "pkill", "hostapd")
        when (result) {
            is WifiRunner.CommandResult.Success -> {
                logger.info("AP stopped")
            }
            else -> {
                logger.error("Failed to stop hostapd")
                return false
            }
        }
        val confPath = configService.getConfig().kammichHostpadPath
        File(confPath).delete()
        return true
    }

    fun getActiveTethering(): WifiTetheringNetwork? {
        val confPath = configService.getConfig().kammichHostpadPath

        val configFile = File(confPath)
        if (!isRunning() || !configFile.exists()) return null

        return try {
            val lines = configFile.readLines()
            val ssid = lines.find { it.startsWith("ssid=") }?.substringAfter("=")
            val channel = lines.find { it.startsWith("channel=") }?.substringAfter("=")?.toIntOrNull()
            // Vi henter frekvensen fra filen hvis den eksisterer som 'frequency='
            val frequency = lines.find { it.startsWith("frequency=") }?.substringAfter("=")?.toIntOrNull()
            val alignedTo = lines.find { it.startsWith("# aligned_to=") }?.substringAfter("=")
            // Vi sjekker om vi har markert den som aligned (f.eks. via en egen flagg-fil eller metadata)
            // Her bruker vi en enkel sjekk: hvis konfigurasjonen er det vi forventer, er den aligned.
            val isAligned = lines.any { it.contains("ieee80211") } // Eksempel på sjekk av alignment-egenskaper

            if (ssid != null) {
                WifiTetheringNetwork(
                    ssid = ssid,
                    channel = channel ?: 0,
                    frequencyMhz = frequency ?: 0, // Nå henter vi denne faktisk
                    isAligned = isAligned,
                    alignedToSSID = alignedTo,
                )
            } else null
        } catch (e: Exception) {
            logger.warn("Kunne ikke parse aktiv hostapd-config: ${e.message}")
            null
        }
    }
}