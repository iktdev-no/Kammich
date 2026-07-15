package no.iktdev.kammich.system.network.wifi

import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.models.internal.config.TetherDevice
import no.iktdev.kammich.models.shared.network.WifiInterfaceInfo
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiSecurityType
import no.iktdev.kammich.models.shared.network.WifiTetherInterface
import no.iktdev.kammich.models.shared.network.WifiTetherSetting
import no.iktdev.kammich.models.shared.network.WifiTethering
import no.iktdev.kammich.models.shared.network.WifiTetheringNetwork
import no.iktdev.kammich.models.shared.network.WifiTetheringState
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.system.exceptions.TetherDeviceNotEnabledException
import no.iktdev.kammich.system.exceptions.TetherDeviceNotFoundException
import no.iktdev.kammich.system.network.wifi.strategy.ap.AccessPointStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Service
class WifiTetherService(
    private val strategies: List<AccessPointStrategy>,
    private val interfaces: WifiInterfaces,
    private val registry: WifiStateRegistry,
    private val sseManager: SseManager,
    private val configService: ConfigService,
) {
    private val log = LoggerFactory.getLogger(WifiTetherService::class.java)

    init {
        val tether = getWifiTether()
        registry.tetheringCurrentTether.store(tether)
    }

    private fun getStrategy(): AccessPointStrategy {
        return strategies.firstOrNull { it.isSupported() }
            ?: throw IllegalStateException("Ingen støttet AP-strategi funnet")
    }

    private fun getTetherDevice(): TetherDevice {
        val tetherIface = configService.getConfig().tetherDevice
            ?: throw TetherDeviceNotFoundException("No Tethering interface configured")
        if (!tetherIface.enabled)
            throw TetherDeviceNotEnabledException("No Tethering interface configured")
        return tetherIface
    }

    fun startTethering() {
        val tetherDevice = getTetherDevice()
        val useInterface = getInterfaceByDeviceId(tetherDevice.deviceId, tetherDevice.enabled) ?: run {
            throw TetherDeviceNotFoundException("Could not find Tethering interface configured")
        }
        val useAPSettings = configService.getConfig().tetherSetting

        log.info("Starter AP på ${useInterface.name}")
        setState(useInterface, WifiTetheringState.STARTING)

        CompletableFuture.runAsync {
            try {
                val startResult = getStrategy().start(useInterface.name, useAPSettings)
                if (startResult is WifiRunner.CommandResult.Success) {
                    val active = getStrategy().getActiveTethering(useInterface.name)
                    if (active == null) {
                        log.info("No active Tethering received for $useInterface.name after connecting")
                    }
                    setState(useInterface, WifiTetheringState.RUNNING, active)

                } else {
                    val error = startResult as WifiRunner.CommandResult.Failure
                    log.error("Failed to start AP\n${error.error}")
                    setState(useInterface, WifiTetheringState.ERROR)
                }
            } catch (e: Exception) {
                log.error("Feil ved start av AP", e)
                setState(useInterface, WifiTetheringState.ERROR)
            }
        }
    }

    fun startAlignedTethering(network: WifiNetwork) {
        val tetherDevice = getTetherDevice()
        val useInterface = getInterfaceByDeviceId(tetherDevice.deviceId, tetherDevice.enabled) ?: run {
            throw TetherDeviceNotFoundException("Could not find Tethering interface configured")
        }
        val useAPSettings = configService.getConfig().tetherSetting
        log.info("Starter aligned tethering mot ${network.ssid}")
        setState(useInterface, WifiTetheringState.ALIGNING)

        CompletableFuture.runAsync {
            try {
                val active = getStrategy().startAligned(useInterface.deviceId, useAPSettings, network)
                setState(useInterface, WifiTetheringState.RUNNING_ALIGNED, active)
            } catch (e: Exception) {
                log.error("Feil ved alignment", e)
                setState(useInterface, WifiTetheringState.ERROR)
                // Forsøk opprydding ved feil
                getStrategy().stop(useInterface.deviceId)
            }
        }
    }

    fun stopTethering(interfaceName: String? = null) {
        fun performStop(interfaceName: WifiTetherInterface): Boolean {
            log.info("Stopper tethering på $interfaceName")
            return try {
                val stopped = getStrategy().stop(interfaceName.name)
                setState(interfaceName, if (stopped) WifiTetheringState.IDLE else WifiTetheringState.ERROR)
                stopped
            } catch (e: Exception) {
                log.error("Feil ved stopp av AP", e)
                false
            }
        }
        if (interfaceName != null) {
            val useInterface = getTetherInterface(interfaceName) ?: run {
                throw TetherDeviceNotFoundException("Could not find Tethering interface configured")
            }
            val success = performStop(useInterface)
            if (success) {
                return
            }
        }
        findTetherDevice()?.let {
            performStop(it)
        }
    }

    fun setState(iface: WifiTetherInterface, state: WifiTetheringState, config: WifiTetheringNetwork? = null) {
        val newState = WifiTethering(
            iface = iface,
            state = state,
            network = config
        )
        registry.tetheringCurrentTether.store(newState)
        updateSSE()
    }



    @OptIn(ExperimentalAtomicApi::class)
    fun getSSEPayload(): Map<String, Any?> {
        val currentTether = registry.tetheringCurrentTether.load()
        return mapOf(
            "type" to "wifi-tethering",
            "payload" to currentTether
        )
    }

    private fun updateSSE() {
        sseManager.send(getSSEPayload())
    }


    fun saveTetherConfig(ssid: String, password: String, security: WifiSecurityType) {
        configService.updateConfig { config ->
            config.copy(tetherSetting = WifiTetherSetting(ssid, password, security))
        }
    }

    fun saveTetherDevice(enabled: Boolean, deviceId: String) {
        configService.updateConfig { config ->
            config.copy(tetherDevice = TetherDevice(enabled, deviceId))
        }
        registry.tetheringCurrentTether.store(getWifiTether())
        sseManager.send(getSSEPayload())
    }

    fun removeTetherDevice(deviceId: String) {
        if (registry.tetheringCurrentTether.load() != null) {
            stopTethering()
        }
        configService.updateConfig { config ->
            config.copy(tetherDevice = null)
        }
        registry.tetheringCurrentTether.store(null)
        sseManager.send(getSSEPayload())
    }


    fun findTetherDevice(): WifiTetherInterface? {
        val devices = configService.getConfig().tetherDevice ?: run {
            log.error("No TetherDevice found in config")
            return null
        }
        val iface = interfaces.getInterfaces().find { it -> it.deviceId == devices.deviceId } ?: run {
            log.error("No Tethering Interface found for $devices.")
            return null
        }
        return WifiTetherInterface(
            name = iface.interfaceName,
            deviceId = iface.deviceId,
            enabled = devices.enabled,
            supportsAp = iface.supportsAp,
            supportsApAndStationSimultaneously = iface.supportsApAndStationSimultaneously,
        )
    }

    fun getAvailableTetherDevices(): List<WifiTetherInterface> {
        return interfaces.getInterfaces().filter { it.deviceId != findTetherDevice()?.deviceId }
            .map { it -> WifiTetherInterface(
                name = it.interfaceName,
                deviceId = it.deviceId,
                enabled = false,
                supportsAp = it.supportsAp,
                supportsApAndStationSimultaneously = it.supportsApAndStationSimultaneously,
            ) }
    }


    fun getTetherSettings(): WifiTetherSetting = configService.getConfig().tetherSetting

    private fun getInterfaceByDeviceId(deviceId: String, enabled: Boolean = true): WifiTetherInterface? {
        return interfaces.getInterfaces().find { it.deviceId == deviceId }?.let {
            WifiTetherInterface(
                name = it.interfaceName,
                deviceId = it.deviceId,
                enabled = enabled,
                supportsAp = it.supportsAp,
                supportsApAndStationSimultaneously = it.supportsApAndStationSimultaneously,
            )
        }
    }

    fun getTetherInterface(interfaceName: String, enabled: Boolean = true): WifiTetherInterface? {
        return interfaces.getInterfaces().find { it.interfaceName == interfaceName }?.let {
            WifiTetherInterface(
                name = it.interfaceName,
                deviceId = it.deviceId,
                enabled = enabled,
                supportsAp = it.supportsAp,
                supportsApAndStationSimultaneously = it.supportsApAndStationSimultaneously,
            )
        }
    }

    fun getWifiTether(): WifiTethering? {
        val configuredDevice = configService.getConfig().tetherDevice ?: run {
            log.error("No Tethering configuration found in config")
            return null
        }
        val configuredInterface = getInterfaceByDeviceId(configuredDevice.deviceId) ?: run {
            log.error("Tether interface ${configuredDevice.deviceId} configured is not available in the system")
            return null
        }

        val activeTether = getStrategy().getActiveTethering(configuredInterface.name)

        val state = when {
            activeTether == null -> WifiTetheringState.IDLE
            activeTether.isAligned -> WifiTetheringState.RUNNING_ALIGNED
            else -> WifiTetheringState.RUNNING
        }

        return WifiTethering(
            iface = WifiTetherInterface(
                name = configuredInterface.name,
                deviceId = configuredDevice.deviceId,
                enabled = true,
                supportsAp = configuredInterface.supportsAp,
                supportsApAndStationSimultaneously = configuredInterface.supportsApAndStationSimultaneously,
            ),
            state = state,
            network = activeTether
        )
    }

}