package no.iktdev.kammich.system.network.wifi.strategy.connection

import no.iktdev.kammich.models.shared.network.WifiConnectionResult
import no.iktdev.kammich.models.shared.network.ConnectivityState
import no.iktdev.kammich.models.shared.network.WifiInterfaceState
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.system.network.wifi.WifiRunner
import no.iktdev.kammich.system.network.wifi.parser.NmcliHelper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NmcliConnectionStrategy(private val runner: WifiRunner) : WifiConnectionStrategy, NmcliHelper() {
    private val log = LoggerFactory.getLogger(NmcliConnectionStrategy::class.java)


    override fun connect(interfaceName: String, ssid: String, password: String?): WifiConnectionResult {
        val command = mutableListOf("sudo", "nmcli", "device", "wifi", "connect", ssid, "ifname", interfaceName)
        if (!password.isNullOrBlank()) command.addAll(listOf("password", password))

        // 1. Prøv å koble til
        val result = runner.run(*command.toTypedArray())

        if (result is WifiRunner.CommandResult.Success) {
            return WifiConnectionResult(true, "Tilkoblet", ConnectivityState.CONNECTED)
        }

        // 2. Hvis det feiler med "property is missing", er profilen korrupt
        val errorOutput = (result as? WifiRunner.CommandResult.Failure)?.error ?: ""
        if (errorOutput.contains("property is missing")) {
            log.warn("Profil for $ssid er korrupt (property missing). Sletter profil og prøver på nytt.")

            // Slett den korrupte profilen med samme navn som SSID
            runner.run("sudo", "nmcli", "connection", "delete", ssid)

            // 3. Prøv én gang til med en ren profil
            val retryResult = runner.run(*command.toTypedArray())
            if (retryResult is WifiRunner.CommandResult.Success) {
                return WifiConnectionResult(true, "Tilkoblet (etter rens)", ConnectivityState.CONNECTED)
            }
        }

        log.error("Failed to connect to $interfaceName using nmcli, with error: $errorOutput")
        return WifiConnectionResult(false, errorOutput, ConnectivityState.FAILED)
    }

    override fun disconnect(interfaceName: String): WifiConnectionResult {
        val command = listOf("sudo", "nmcli", "device", "disconnect", interfaceName)

        return when (val result = runner.run(*command.toTypedArray())) {
            is WifiRunner.CommandResult.Success -> WifiConnectionResult(true, "Koblet fra", ConnectivityState.DISCONNECTED)
            is WifiRunner.CommandResult.Failure -> WifiConnectionResult(false, result.error, ConnectivityState.FAILED)
        }
    }

    override fun getState(interfaceName: String): WifiInterfaceState {
        // 0 Sjekk om vi er i AP føst
        val iwInfo = runner.run("iw", "dev", interfaceName, "info")
        if (iwInfo is WifiRunner.CommandResult.Success && iwInfo.output.contains("type AP")) {
            // Vi er en basestasjon, ikke en klient. Returner DISCONNECTED.
            return WifiInterfaceState(interfaceName, ConnectivityState.DISCONNECTED, null)
        }

        // 1. Sjekk status
        val statusResult = runner.run("nmcli", "-t", "-f", "GENERAL.STATE,GENERAL.CONNECTION", "device", "show", interfaceName)
        val isConnected = statusResult is WifiRunner.CommandResult.Success && statusResult.output.contains("GENERAL.STATE:100")

        if (!isConnected) {
            return WifiInterfaceState(interfaceName, ConnectivityState.DISCONNECTED, null)
        }

        val connName = (statusResult as WifiRunner.CommandResult.Success).output.lines()
            .find { it.startsWith("GENERAL.CONNECTION:") }
            ?.substringAfter(":") ?: return WifiInterfaceState(interfaceName, ConnectivityState.DISCONNECTED, null)

        // 2. Hent detaljer
        val details = runner.run("nmcli", "-t", "-f", "802-11-wireless.ssid,802-11-wireless-security.key-mgmt", "connection", "show", connName)
            .map { output ->
                output.lines().associate {
                    val p = it.split(":", limit = 2)
                    p[0] to (p.getOrNull(1) ?: "")
                }
            } ?: emptyMap()

        // 3. Hent Signal og BSSID (Skuddsikker parsing)
        val activeLine = runner.run("nmcli", "-t", "-f", "IN-USE,BSSID,SIGNAL", "dev", "wifi")
            .map { output -> output.lines().find { it.startsWith("*:") } ?: "" } ?: ""

        // Linjen ser ut som: *:0C:EA:14:CA:E4:97:56 (hvor 56 er signalet)
        val bssid = if (activeLine.length > 5) {
            activeLine.removePrefix("*:") // Fjerner *:
                .substringBeforeLast(":") // Tar alt før den siste kolonen
        } else "00:00:00:00:00:00"

        val signal = if (activeLine.isNotEmpty()) {
            activeLine.substringAfterLast(":").toIntOrNull() ?: 0
        } else 0

        val rawSsid = details["802-11-wireless.ssid"] ?: connName
        val isHidden = rawSsid == "" || rawSsid == "--"

        val freqResult = runner.run("nmcli", "-t", "-f", "GENERAL.FREQ", "device", "show", interfaceName)
        val freqMhz = freqResult.let { (it as? WifiRunner.CommandResult.Success)?.output?.substringAfter(":")?.replace(" MHz", "")?.toIntOrNull() }

        // Hjelpefunksjon for å utlede data fra frekvens
        fun getChannel(f: Int?): Int? = f?.let {
            when {
                it in 2412..2484 -> ((it - 2412) / 5) + 1
                it in 5170..5825 -> ((it - 5170) / 5) + 34
                else -> null
            }
        }

        return WifiInterfaceState(
            interfaceName = interfaceName,
            connectivityState = ConnectivityState.CONNECTED,
            network = WifiNetwork(
                ssid = rawSsid,
                signalPercent = signal,
                isSecure = !details["802-11-wireless-security.key-mgmt"].isNullOrBlank(),
                bssid = bssid,
                securityType = details["802-11-wireless-security.key-mgmt"] ?: "none",
                interfaceName = interfaceName,
                isHidden = isHidden,
                channel = getChannel(freqMhz),
                frequencyMhz = freqMhz,
                hwMode = if (freqMhz != null && freqMhz < 5000) "g" else "a"
            )
        )
    }

}