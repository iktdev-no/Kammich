package no.iktdev.kammich.system.network.wifi.strategy.connection

import no.iktdev.kammich.models.shared.network.InterfaceActiveState
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiNetworkConnection
import no.iktdev.kammich.system.network.wifi.WifiRunner
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class WpaCliConnectionStrategy(private val runner: WifiRunner) : WifiConnectionStrategy {

    private val log = LoggerFactory.getLogger(WpaCliConnectionStrategy::class.java)


    override fun isSupported(): Boolean {
        // Sjekk om wpa_cli finnes
        return try {
            Runtime.getRuntime().exec("which wpa_cli").waitFor() == 0
        } catch (e: Exception) { false }
    }

    override fun getState(interfaceName: String): WifiNetworkConnection {
        TODO("Not implemented yet")
    }

    override fun connect(interfaceName: String, network: WifiNetwork, password: String?): WifiNetworkConnection {
        val ssid = network.ssid
        // wpa_cli bruker ofte -i <interface>
        // Vi må legge til et nettverk, sette SSID, sette passord, og velge det.
        val cmds = listOf(
            "wpa_cli -i $interfaceName add_network",
            "wpa_cli -i $interfaceName set_network 0 ssid '\"$ssid\"'",
            password?.let { "wpa_cli -i $interfaceName set_network 0 psk '\"$it\"'" },
            "wpa_cli -i $interfaceName enable_network 0",
            "wpa_cli -i $interfaceName select_network 0"
        ).filterNotNull()

        // Her må du kjøre hver kommando sekvensielt via runner
        for (cmd in cmds) {
            val result = runner.run(*cmd.split(" ").toTypedArray())
            if (result is WifiRunner.CommandResult.Failure) {
                log.error("Failed to connect to $interfaceName using wpa_cli")
                return WifiNetworkConnection(interfaceName, InterfaceActiveState.Idle, null)
            }
        }
        log.info("Successfully connected to $interfaceName using wpa_cli")
        return WifiNetworkConnection(interfaceName, InterfaceActiveState.Connected, network)
    }

    override fun disconnect(interfaceName: String): WifiNetworkConnection {
        // 1. Koble fra
        val disconnectCmd = "wpa_cli -i $interfaceName disconnect"
        // 2. Fjern nettverket fra listen (vi antar ID 0 da vi satt den til 0 i connect)
        val removeCmd = "wpa_cli -i $interfaceName remove_network 0"

        val result = runner.run(*disconnectCmd.split(" ").toTypedArray())
        // Vi kjører remove_network uavhengig av om disconnect feilet (for å rydde opp)
        runner.run(*removeCmd.split(" ").toTypedArray())
        if (result is WifiRunner.CommandResult.Failure) {
            throw RuntimeException("Failed to disconnect to $interfaceName using wpa_cli")
        }
        return WifiNetworkConnection(interfaceName, InterfaceActiveState.Idle, null)
    }


}