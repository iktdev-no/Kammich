package no.iktdev.kammich.system.network

import com.google.gson.Gson
import no.iktdev.kammich.services.ConfigService
import no.iktdev.kammich.models.internal.config.SelectedWirelessTetherInterface
import no.iktdev.kammich.models.shared.network.*
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.networking.SSEWifiTether
import no.iktdev.kammich.system.exceptions.TetherDeviceNotFoundException
import no.iktdev.kammich.system.network.strategy.ap.AccessPointStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class WifiTetherServiceV2(
    private val sseManager: SseManager,
    private val configService: ConfigService,
    private val strategies: List<AccessPointStrategy>,
    private val interfaceRegistry: NetworkInterfaceRegistryV2,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val AP_PROFILE_NAME = "kammich-ap"
    }

    private fun getStrategy(): AccessPointStrategy? = strategies.find { it.isSupported() }


    fun acquireTetherDevice(nif: String): Boolean {
        // Bruk acquire for å sikre seg at grensesnittet settes i Tether-modus og beholdes
        val lease = interfaceRegistry.acquire(
            interfaceName = nif,
            requestedMode = NetworkInterfaceMode.Tether,
            onReject = { log.error("Could not obtain TetherDevice for $nif") }
        ) ?: return false

        log.info("Acquired tether lock on ${lease.interfaceName}")
        send(WifiTether(nif, WirelessTetheringState.Acquired, null))
        saveTetherDevice(lease.interfaceName)
        return true
    }

    fun releaseTetherDevice(nif: String): Boolean {
        // Hent ut den aktive leasen for Tether på dette grensesnittet og frigi den manuelt
        val lease = interfaceRegistry.getActiveLease(nif, NetworkInterfaceMode.Tether)
        if (lease != null) {
            lease.release()
            log.info("Release tether lock on ${lease.interfaceName}")
        } else {
            log.warn("Fant ingen aktiv tether-lease å frigi for $nif")
        }

        sseManager.send(SSEWifiTether(nif, null))
        removeTetherDevice()
        return true
    }

    fun startTethering(): Boolean {
        val interfaceName = getConfiguredTetherInterfaceName()
        return startTethering(interfaceName)
    }

    fun startTetheringAsync(interfaceName: String?) {
        CompletableFuture.runAsync {
            if (interfaceName != null) {
                startTethering(interfaceName)
            } else {
                startTethering()
            }
        }
    }

    fun startTethering(interfaceName: String): Boolean {
        val apSettings = configService.getConfig().tetherSetting
        val strategy = getStrategy() ?: run {
            log.warn("Ingen godkjent AP-strategi funnet")
            send(WifiTether(interfaceName, WirelessTetheringState.Idle, null, WirelessTetheringError.DeviceNotFound))
            return false
        }

        saveTetherDeviceInternal(interfaceName)
        send(WifiTether(interfaceName, WirelessTetheringState.Starting, null))

        // Skaff leasen via acquire slik at modusen holdes i sjakk over tid
        val lease = interfaceRegistry.acquire(
            interfaceName = interfaceName,
            requestedMode = NetworkInterfaceMode.Tether,
            onReject = {
                log.error("Klarte ikke å skaffe lease for AP på $interfaceName")
            }
        ) ?: run {
            send(WifiTether(interfaceName, WirelessTetheringState.Idle, null, WirelessTetheringError.DeviceNotFound))
            return false
        }

        var success = false
        try {
            log.info("Starter tethering på ${lease.interfaceName}")
            val result = strategy.start(lease.interfaceName, apSettings, autoconnect = true)
            send(result)
            success = true

            // Hvis start feilet helt i strategien, slipper vi leasen igjen med en gang
            if (result.state != WirelessTetheringState.Tethering && result.state != WirelessTetheringState.Starting) {
                lease.release()
            }
        } catch (e: Exception) {
            log.error("Feil ved start av AP på $interfaceName", e)
            lease.release()
            send(WifiTether(interfaceName, WirelessTetheringState.Idle, null))
        }

        return success
    }

    fun stopTethering(interfaceName: String): Boolean {
        val strategy = getStrategy() ?: run {
            log.warn("Ingen strategi funnet for å stoppe tethering")
            return false
        }

        send(WifiTether(interfaceName, WirelessTetheringState.Stopping, null))

        var success = false
        try {
            interfaceRegistry.releaseLease(interfaceName, NetworkInterfaceMode.Tether) { lease ->
                log.info("Stopper tethering på $interfaceName")
                try {
                    strategy.stop(interfaceName)
                    success = true
                } catch (e: Exception) {
                    val errorMsg = e.message ?: ""
                    if (errorMsg.contains("not active", ignoreCase = true)) {
                        log.info("Enheten $interfaceName var allerede inaktiv under stopp, ansees som stoppet.")
                        success = true
                    } else {
                        log.error("Strategi-stopp feilet for $interfaceName", e)
                        throw e
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Stopp av AP feilet med exception", e)
        }

        val error = if (success) null else WirelessTetheringError.StopFailed
        val result = WifiTether(interfaceName, WirelessTetheringState.Idle, null, error)

        send(result)

        return success
    }

    private fun getConfiguredTetherInterfaceName(): String {
        val tetherDevice = configService.getConfig().selectedWirelessTetherInterface
            ?: throw TetherDeviceNotFoundException("Ingen enhet er konfigurert for tethering")
        return interfaceRegistry.findInterface(mac = tetherDevice.deviceId)?.interfaceName
            ?: throw TetherDeviceNotFoundException("Konfigurert tether-enhet ble ikke funnet")
    }

    fun send(result: WifiTether) {
        log.info("Sender ${Gson().toJson(result)}")
        sseManager.send(SSEWifiTether(result.ifName, result))
    }

    fun getCurrentState(): List<WifiInterfaceTether> {
        val strategy = getStrategy()

        return interfaceRegistry.getInterfaces(NetworkInterfaceType.Wifi, NetworkInterfaceMode.Tether)
            .map { (nif, isAvailable) ->
                val name = nif.interfaceName
                val caps = (nif as? WirelessNetworkInterface)?.caps ?: emptySet()

                val tetherState = strategy?.getState(name) ?: WifiTether(ifName = name, state = WirelessTetheringState.Idle, null)

                val network = if (strategy != null && tetherState.state == WirelessTetheringState.Tethering) {
                    strategy.getNetwork(name)
                } else {
                    null
                }

                WifiInterfaceTether(
                    name = name,
                    isUsable = isAvailable,
                    operatingMode = nif.mode,
                    state = tetherState.state,
                    network = network,
                    caps = caps
                )
            }
    }

    // --- Konfigurasjon og Lagring ---

    fun saveTetherConfig(ssid: String, password: String, security: WifiSecurityType) {
        configService.updateConfig { config ->
            config.copy(tetherSetting = WifiTetherAP(ssid, password, security))
        }
    }

    fun saveTetherDevice(ifaceName: String) {
        saveTetherDeviceInternal(ifaceName)
    }

    private fun saveTetherDeviceInternal(ifaceName: String) {
        val device = interfaceRegistry.findInterface(name = ifaceName)
            ?: throw RuntimeException("Fant ingen enhet for $ifaceName")

        configService.updateConfig { config ->
            config.copy(selectedWirelessTetherInterface = SelectedWirelessTetherInterface(true, device.macAdress))
        }
    }

    fun removeTetherDevice() {
        configService.updateConfig { config ->
            config.copy(selectedWirelessTetherInterface = null)
        }
    }

    fun getTetherSettings(): WifiTetherAP = configService.getConfig().tetherSetting
}