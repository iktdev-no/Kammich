package no.iktdev.kammich.system.network.wifi.connectivity

import no.iktdev.kammich.models.shared.network.WifiConnectionResult
import no.iktdev.kammich.models.shared.network.WifiConnectivityState
import no.iktdev.kammich.models.shared.network.WifiInterfaceState
import no.iktdev.kammich.system.network.wifi.WifiRunner
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Order(1)
@Component
class IwctlConnectionStrategy(private val runner: WifiRunner) : WifiConnectionStrategy {
    private val log = LoggerFactory.getLogger(IwctlConnectionStrategy::class.java)

    override fun isSupported(): Boolean {
        // Sjekk direkte etter iwctl, som er klienten for iwd
        return java.io.File("/usr/bin/iwctl").exists()
    }

    override fun connect(interfaceName: String, ssid: String, password: String?): WifiConnectionResult {
        // iwctl syntax: iwctl --passphrase <passord> station <iface> connect <ssid>
        // Merk: iwctl krever noen ganger at passordet er i hermetegn hvis det inneholder spesialtegn.
        val command = mutableListOf("iwctl", "--passphrase", password ?: "", "station", interfaceName, "connect", ssid)
        
        return when (val result = runner.run(*command.toTypedArray())) {
            is WifiRunner.CommandResult.Success -> {
                log.info("Successfully connected to $interfaceName using iwctl")
                WifiConnectionResult(true, "Tilkoblet $ssid via iwctl", WifiConnectivityState.CONNECTED)
            }
            is WifiRunner.CommandResult.Failure -> {
                log.error("Failed to connect to $interfaceName using iwctl")
                WifiConnectionResult(false, result.error, WifiConnectivityState.FAILED)
            }
        }
    }

    override fun disconnect(interfaceName: String): WifiConnectionResult {
        // iwctl station <iface> disconnect
        val command = listOf("iwctl", "station", interfaceName, "disconnect")

        return when (val result = runner.run(*command.toTypedArray())) {
            is WifiRunner.CommandResult.Success -> {
                WifiConnectionResult(true, "Koblet fra", WifiConnectivityState.DISCONNECTED)
            }
            is WifiRunner.CommandResult.Failure -> {
                WifiConnectionResult(false, result.error, WifiConnectivityState.FAILED)
            }
        }
    }

    override fun getState(interfaceName: String): WifiInterfaceState {
        TODO("Not yet implemented")
    }
}