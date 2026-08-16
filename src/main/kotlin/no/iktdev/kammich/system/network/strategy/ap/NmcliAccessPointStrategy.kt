package no.iktdev.kammich.system.network.strategy.ap

import com.google.gson.Gson
import no.iktdev.kammich.models.shared.network.InterfaceMode
import no.iktdev.kammich.models.shared.network.WifiTetherAP
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiSecurityType
import no.iktdev.kammich.models.shared.network.WifiTether
import no.iktdev.kammich.models.shared.network.WirelessTetheringError
import no.iktdev.kammich.models.shared.network.WirelessTetheringState
import no.iktdev.kammich.system.SysCommand
import no.iktdev.kammich.system.network.al.INmcliAL
import no.iktdev.kammich.system.network.WifiTetherServiceV2
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NmcliAccessPointStrategy(
    private val exec: SysCommand,
    private val nmcliAL: INmcliAL
) : AccessPointStrategy {

    private val log = LoggerFactory.getLogger(javaClass)

    fun setAutoconnect(profileName: String, enable: Boolean): Boolean {
        val result = nmcliAL.setAutoConnect(profileName, enable)
        if (result) {
            log.info("Autoconnect set to $enable for $profileName")
        } else {
            log.error("Failed to set autoconnect for $profileName: $result")
        }
        return result
    }

    // Eksempel på utvidelse i NmcliAccessPointStrategy
    override fun start(interfaceName: String, tether: WifiTetherAP, autoconnect: Boolean): WifiTether {
        // Valider passord på forhånd hvis det kreves sikkerhet
        if (tether.security != WifiSecurityType.NONE) {
            if (tether.password.isBlank() || tether.password.length < 8) {
                return WifiTether(ifName = interfaceName, network = null, error = WirelessTetheringError.PasswordTooShort)
            }
        }

        nmcliAL.deleteConnection(WifiTetherServiceV2.AP_PROFILE_NAME)

        val createResult = nmcliAL.createWifiTetherConnection(
            ifName = interfaceName,
            connectionName = WifiTetherServiceV2.AP_PROFILE_NAME,
            ssid = tether.ssid,
            password = tether.password,
            securityType = tether.security.name,
            autoConnect = autoconnect
        )

        if (!createResult.isSuccess()) {
            val asFailure = createResult as SysCommand.Result.Failure
            log.error("Unable to create NMCLI access point profile: ${asFailure.errOutput}")

            val error = when {
                asFailure.errOutput?.contains("property is invalid", ignoreCase = true) == true -> WirelessTetheringError.PasswordTooShort
                asFailure.errOutput?.contains("invalid", ignoreCase = true) == true -> WirelessTetheringError.InvalidSettings
                else -> WirelessTetheringError.StartFailed
            }
            return WifiTether(ifName = interfaceName, network = null, error = error)
        }

        val connectResult = nmcliAL.connect(WifiTetherServiceV2.AP_PROFILE_NAME)
        if (!connectResult.isSuccess()) {
            return WifiTether(ifName = interfaceName, network = null, error = WirelessTetheringError.StartFailed)
        }

        val network = getNetwork(interfaceName)
        return WifiTether(ifName = interfaceName, WirelessTetheringState.Tethering, network = network, error = null)
    }

    override fun stop(interfaceName: String): Boolean {
        val activeConnection = nmcliAL.getConnectionName(interfaceName)
        if (!activeConnection.isNullOrBlank()) {
            log.info("Dropping active connection '$activeConnection' on interface $interfaceName")
            nmcliAL.dropConnection(activeConnection)

            if (activeConnection != WifiTetherServiceV2.AP_PROFILE_NAME) {
                try {
                    nmcliAL.deleteConnection(activeConnection)
                } catch (e: Exception) {
                    log.debug("Kunne ikke slette tilkobling $activeConnection: ${e.message}")
                }
            }
        }

        try {
            nmcliAL.deleteConnection(WifiTetherServiceV2.AP_PROFILE_NAME)
        } catch (e: Exception) {
            log.debug("Standard AP-profil fantes ikke eller kunne ikke slettes: ${e.message}")
        }

        // Siden vi allerede har droppet tilkoblingen, er enheten garantert på vei ned.
        // Vi slipper å kalle nmcli device disconnect og risikere exit code 6!
        log.info("Stopped AP on $interfaceName via profile teardown")
        return true
    }

    override fun isSupported(): Boolean {
        return exec.nonSudo("which", "nmcli").isSuccess()
    }

    override fun getState(interfaceName: String): WifiTether {
        val connectionName = nmcliAL.getConnectionName(interfaceName)
            ?: return WifiTether(interfaceName, WirelessTetheringState.Idle, null)

        val mode = nmcliAL.getWirelessMode(connectionName)
        // Vi definerer tilstand basert på om modusen er Tether
        val state = if (mode == InterfaceMode.Tether) WirelessTetheringState.Tethering else WirelessTetheringState.Idle
        val network = getNetwork(interfaceName)

        return WifiTether(
            ifName = interfaceName,
            state = state,
            network = network,
        )
    }

    override fun getNetwork(interfaceName: String): WifiNetwork? {
        // Først: dobbeltsjekk at vi faktisk er i tether-mode
        val connectionName = nmcliAL.getConnectionName(interfaceName) ?: return null
        val mode = nmcliAL.getWirelessMode(connectionName)

        if (mode != InterfaceMode.Tether) {
            return null
        }

        // Deretter: Hent nettverksdetaljer
        val networks = nmcliAL.getNetworks(interfaceName)
        return networks.find { it.interfaceName == interfaceName && (it.isActive || it.inUse) }
            ?: networks.firstOrNull { it.interfaceName == interfaceName }
    }
}