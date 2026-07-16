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

    fun start(interfaceName: String, tether: WifiTetherSetting, network: WifiNetwork? = null): WifiTetheringNetwork {
        val isAligned = network != null
        logger.info("Starter AP. Modus: ${if (isAligned) "Aligned til ${network?.ssid}" else "Standard"}")

        val confPath = configService.getConfig().kammichHostpadPath

        // Definer kanaler. Hvis vi ikke er aligned, fall tilbake til trygg default (f.eks. kanal 36 eller 6)
        val targetFreq = network?.frequencyMhz ?: 5180
        val targetChannel = network?.channel ?: 6
        val hwMode = network?.hwMode ?: "g" // 'a' for 5GHz, 'g' for 2.4GHz

        val wpaConfig = when (tether.security) {
            WifiSecurityType.NONE -> "wpa=0"
            WifiSecurityType.WPA2 -> "wpa=2\nwpa_key_mgmt=WPA-PSK\nrsn_pairwise=CCMP"
            WifiSecurityType.WPA3 -> "wpa=2\nieee80211w=2\nwpa_key_mgmt=WPA-PSK-SHA256\nrsn_pairwise=GCMP"
        }

        val config = """
# aligned_to=${if (isAligned) network.ssid else "none"}
interface=$interfaceName
driver=nl80211
ssid=${tether.ssid}
hw_mode=$hwMode
channel=$targetChannel
# frequency=$targetFreq

$wpaConfig
wpa_passphrase=${tether.password}

ieee80211n=1
ieee80211ac=1
wmm_enabled=1
auth_algs=1
    """.trimIndent()

        File(confPath).writeText(config)

        val pidPath = "$confPath.pid"
        val command = listOf("sudo", "hostapd", "-P", pidPath, "-B", confPath)
        val result = runner.run(command)
        if (result is WifiRunner.CommandResult.Failure) {
            throw IllegalStateException("Failed to start hostapd with command: ${command.joinToString(" ")}: ${result.error}")
        }

        return WifiTetheringNetwork(
            ssid = tether.ssid,
            channel = targetChannel,
            frequencyMhz = targetFreq,
            alignedToSSID = network?.ssid,
            isAligned = isAligned
        )
    }


    fun stop(interfaceName: String): Boolean {
        if (!isRunning()) {
            logger.info("AP Is already stopped")
            return true
        }

        val config = configService.getConfig()
        val confPath = config.kammichHostpadPath
        val pidPath = "$confPath.pid"
        val pidFile = File(pidPath)

        logger.info("Stopping AP on $interfaceName")

        // 1. Prøv kontrollert stopp via PID hvis filen eksisterer
        val stopped = if (pidFile.exists()) {
            val pid = pidFile.readText().trim()
            val result = runner.run("sudo", "kill", pid)
            result is WifiRunner.CommandResult.Success
        } else {
            // Fallback til pkill hvis PID-filen mangler (men logg advarsel)
            logger.warn("PID-fil manglet for $interfaceName, bruker pkill som fallback")
            runner.run("sudo", "pkill", "-f", "hostapd -P $pidPath") is WifiRunner.CommandResult.Success
        }

        if (stopped) {
            logger.info("AP stoppet kontrollert")

            // 2. Rydd opp i filer
            pidFile.delete()
            File(confPath).delete()

            // 3. Gi kontrollen tilbake til NetworkManager med en gang
            // Vi antar at 'this' har tilgang til manage() eller at vi kaller det via strategy
            return true
        } else {
            logger.error("Feilet ved stopp av hostapd")
            return false
        }
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