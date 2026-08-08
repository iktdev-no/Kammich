package no.iktdev.kammich.system.network.wifi

import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.models.internal.config.SelectedWirelessTetherInterface
import no.iktdev.kammich.models.internal.network.WifiInterfaceState
import no.iktdev.kammich.models.internal.network.asWifi
import no.iktdev.kammich.models.internal.network.setTethering
import no.iktdev.kammich.models.shared.network.*
import no.iktdev.kammich.models.shared.network.old.*
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.SSEWifiTethering
import no.iktdev.kammich.system.exceptions.TetherDeviceNotFoundException
import no.iktdev.kammich.system.network.NetworkInterfaceRegistry
import no.iktdev.kammich.system.network.NetworkStateRepository
import no.iktdev.kammich.system.network.wifi.strategy.ap.AccessPointStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Service
class WifiTetherService(
    private val sseManager: SseManager,
    private val configService: ConfigService,
    private val strategy: List<AccessPointStrategy>,
    private val interfaceRegistry: NetworkInterfaceRegistry,
    private val repository: NetworkStateRepository
) {
    private val log = LoggerFactory.getLogger(WifiTetherService::class.java)
    val networkInterfaceMode = NetworkInterfaceMode.Master

    companion object {
        val ap_profileName = "kammich-ap"
    }

    fun getStrategy(): AccessPointStrategy? {
        return strategy.find { it.isSupported() }
    }

    fun getCurrentState(): List<WifiNetworkTether> {
        return getWifiTetheringInterfaces().map { it ->
            WifiNetworkTether(
                it.name,
                network = it.tethering?.network,
                state = it.tethering?.state ?: WirelessTetheringState.Idle,
            )
        }
    }

    fun getWifiTetheringInterfaces(): List<WirelessInterface> {
        val ifaces = interfaceRegistry.getInterfaces(NetworkInterfaceType.Wifi, NetworkInterfaceMode.Master)
            .map { (iface, state, isAvailable) ->
                val wifiState = state?.asWifi()
                WirelessInterface(
                    name = iface.interfaceName,
                    address = iface.macAdress,
                    isAvailable = isAvailable,
                    operatingState = wifiState?.toWirelessOperatingState() ?: WirelessOperatingState.Idle,
                    search = wifiState?.scanToWirelessNetworkSearch(),
                    connection = wifiState?.connectionToWirelessConnection(),
                    tethering = wifiState?.tetheringToWirelessTethering()
                )
            }
        return ifaces
    }

    private fun getTetherDeviceIfName(): String {
        val tetherDeivce = configService.getConfig().selectedWirelessTetherInterface
            ?: throw TetherDeviceNotFoundException("No Tethering interface configured")
        return interfaceRegistry.findInterface(mac = tetherDeivce.deviceId)?.interfaceName ?: throw TetherDeviceNotFoundException("No Tethering interface configured")
    }



    fun startTethering(interfaceName: String) {
        val useInterface = getWifiTetheringInterfaces().find { it.name == interfaceName }
        if (useInterface?.name != interfaceName) {
            log.error("Interface is not configured for tethering $interfaceName")
            return
        }

        val useAPSettings = configService.getConfig().tetherSetting
        val strategy = getStrategy() ?: run {
            log.warn("No Tethering strategy found")
            return
        }


        val onRejectLease = {
            log.error("Could not obtain lease to start tether on ${useInterface.name}")
        }

        interfaceRegistry.obtain(interfaceName, networkInterfaceMode, onRejectLease) { lease ->
            val name =  lease.getInterfaceName()
            log.info("Lease obtained on ${lease.getInterfaceName()}")
            log.info("Starter AP på ${name}")
            lease.update({ it -> it.setTethering(InterfaceActiveState.StartingTether, null )}) {
                updateSSE()
            }
            CompletableFuture.runAsync {
                try {
                    val result = strategy.start(useInterface.name, useAPSettings, true)
                    val network = if (result.isSuccess()) {
                        strategy.getState(useInterface.name).network
                    } else {
                        log.error("Could not start tether on ${useInterface.name}")
                        null
                    }
                    lease.update({it.setTethering(InterfaceActiveState.Tethering, WifiNetworkTether(
                        name = name,
                        state = WirelessTetheringState.Broadcasting,
                        network = network
                    ))}) {
                        updateSSE()
                    }
                } catch (e: Exception) {
                    log.error("Feil ved start av AP", e)
                    lease.update({it.setTethering(InterfaceActiveState.Idle, null)}) {
                        updateSSE()
                    }
                }
            }

        }
    }

    fun stopTethering(interfaceName: String) {
        val strategy = getStrategy() ?: run {
            log.warn("No Tethering strategy found")
            return
        }

        val onRejeact = {
            log.error("Could not obtain lease to terminate tethering $interfaceName")
        }

        interfaceRegistry.obtain(interfaceName, networkInterfaceMode, onRejeact) { lease ->
            log.info("Stopper tethering på ${lease.getInterfaceName()}")
            val result = try {
                strategy.stop(interfaceName)
            } catch (e: Exception) {
                log.error("Error stopping tether for ${lease.getInterfaceName()}", e)
            }
            lease.update({it.setTethering(InterfaceActiveState.Idle, null)}) {
                updateSSE()
            }
            lease.release()
        }
    }



    @OptIn(ExperimentalAtomicApi::class)
    fun getSSETetheringPayload(): SSEWifiTethering {
        // WifiNetworkTether

        val wifiInterfaces = repository.getCurrentState().interfaces
            .filterValues { it is WifiInterfaceState } // Filtrer først
            .mapValues { it.value as WifiInterfaceState } // Cast til rett type
            .map { (name, state) ->
                WifiNetworkTether(
                    name = name,
                    state = state.tethering?.state ?: WirelessTetheringState.Idle,
                    network = state.tethering?.network
                )
            }

        if (wifiInterfaces.size > 1) {
            log.error("Why do we have more than one tethering interface? Cap should be ONE. O N E")
        }

        var useTetherItem = wifiInterfaces.firstOrNull()

        if (useTetherItem == null) {
            try {
                val device = getTetherDeviceIfName()
                useTetherItem = WifiNetworkTether(
                    name = device,
                    state = WirelessTetheringState.Idle
                )
            } catch (e: Exception) {
            }
        }

        return SSEWifiTethering(useTetherItem)
    }

    private fun updateSSE() {
        sseManager.send(getSSETetheringPayload())
    }


    fun saveTetherConfig(ssid: String, password: String, security: WifiSecurityType) {
        configService.updateConfig { config ->
            config.copy(tetherSetting = WifiTetherAP(ssid, password, security))
        }
    }

    fun saveTetherDevice(ifaceName: String) {
        val device = try {
            interfaceRegistry.findInterface(name = ifaceName)
        } catch (e: Exception) {
            return
        }
        if (device == null) {
            throw RuntimeException("No Tethering device found for $ifaceName")
        }
        configService.updateConfig { config ->
            config.copy(selectedWirelessTetherInterface = SelectedWirelessTetherInterface(true, device.macAdress))
        }
        sseManager.send(getSSETetheringPayload())
    }

    fun removeTetherDevice(deviceId: String) {
        configService.updateConfig { config ->
            config.copy(selectedWirelessTetherInterface = null)
        }
        sseManager.send(getSSETetheringPayload())
    }



    fun getTetherSettings(): WifiTetherAP = configService.getConfig().tetherSetting

}