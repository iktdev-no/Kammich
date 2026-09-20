package no.iktdev.kammich.system.network.strategy.ap

import no.iktdev.kammich.models.internal.network.NmCliDeviceState
import no.iktdev.kammich.models.shared.network.InterfaceMode
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode
import no.iktdev.kammich.models.shared.network.WifiInterfaceState
import no.iktdev.kammich.models.shared.network.WifiInterfaceStateType
import no.iktdev.kammich.models.shared.network.WifiInterfaceErrorType
import no.iktdev.kammich.models.shared.network.WifiTetherAP
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiSecurityType
import no.iktdev.kammich.system.SysCommand
import no.iktdev.kammich.system.network.wifi.WifiTetherServiceV2
import no.iktdev.kammich.system.network.al.INmcliAL
import no.iktdev.kammich.system.network.strategy.state.NmcliWifiStateStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NmcliAccessPointStrategy(
    private val strategy: NmcliWifiStateStrategy,
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

    override fun start(
        interfaceName: String,
        tether: WifiTetherAP,
        autoconnect: Boolean
    ): WifiInterfaceState {
        if (tether.security != WifiSecurityType.NONE) {
            if (tether.password.isBlank() || tether.password.length < 8) {
                return WifiInterfaceState(
                    ifName = interfaceName,
                    state = WifiInterfaceStateType.Idle,
                    operatingMode = NetworkInterfaceMode.Tether,
                    error = WifiInterfaceErrorType.TetherPasswordTooShort
                )
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
            val failure = createResult as SysCommand.Result.Failure

            log.error(
                "Unable to create NMCLI access point profile: ${failure.errOutput}"
            )

            val error = when {
                failure.errOutput?.contains(
                    "property is invalid",
                    ignoreCase = true
                ) == true -> WifiInterfaceErrorType.TetherPasswordTooShort

                failure.errOutput?.contains(
                    "invalid",
                    ignoreCase = true
                ) == true -> WifiInterfaceErrorType.TetherInvalidSettings

                else -> WifiInterfaceErrorType.TetherStartFailed
            }

            return WifiInterfaceState(
                ifName = interfaceName,
                state = WifiInterfaceStateType.Idle,
                operatingMode = NetworkInterfaceMode.Tether,
                error = error
            )
        }

        val connectResult = nmcliAL.connect(WifiTetherServiceV2.AP_PROFILE_NAME)

        if (!connectResult.isSuccess()) {
            return WifiInterfaceState(
                ifName = interfaceName,
                state = WifiInterfaceStateType.Idle,
                operatingMode = NetworkInterfaceMode.Tether,
                error = WifiInterfaceErrorType.TetherStartFailed
            )
        }

        return WifiInterfaceState(
            ifName = interfaceName,
            state = WifiInterfaceStateType.Tethering,
            operatingMode = NetworkInterfaceMode.Tether,
            network = getNetwork(interfaceName)
        )
    }

    override fun stop(interfaceName: String): Boolean {
        val activeConnection = nmcliAL.getConnectionName(interfaceName)

        if (!activeConnection.isNullOrBlank()) {
            log.info(
                "Dropping active connection '$activeConnection' on interface $interfaceName"
            )

            nmcliAL.dropConnection(activeConnection)

            if (activeConnection != WifiTetherServiceV2.AP_PROFILE_NAME) {
                try {
                    nmcliAL.deleteConnection(activeConnection)
                } catch (e: Exception) {
                    log.debug(
                        "Kunne ikke slette tilkobling $activeConnection: ${e.message}"
                    )
                }
            }
        }

        try {
            nmcliAL.deleteConnection(WifiTetherServiceV2.AP_PROFILE_NAME)
        } catch (e: Exception) {
            log.debug("Standard AP-profil fantes ikke eller kunne ikke slettes: ${e.message}")
        }

        log.info("Stopped AP on $interfaceName via profile teardown")
        return true
    }

    override fun isSupported(): Boolean {
        return exec.nonSudo("which", "nmcli").isSuccess()
    }

    override fun getState(ifName: String): WifiInterfaceState =
        strategy.getState(ifName)

    override fun getNetwork(interfaceName: String): WifiNetwork? =
        strategy.getNetwork(interfaceName)

}