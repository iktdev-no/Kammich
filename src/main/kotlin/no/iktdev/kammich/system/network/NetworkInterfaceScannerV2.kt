package no.iktdev.kammich.system.network

import no.iktdev.kammich.models.internal.network.NmCliDeviceType
import no.iktdev.kammich.models.shared.network.*
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.networking.SSEWifiConnection
import no.iktdev.kammich.sse.events.networking.SSEWifiInterfaceClient
import no.iktdev.kammich.sse.events.networking.SSEWifiInterfaceTether
import no.iktdev.kammich.sse.events.networking.SSEWifiTether
import no.iktdev.kammich.system.network.al.IIwAL
import no.iktdev.kammich.system.network.al.INmcliAL
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

@Service
class NetworkInterfaceScannerV2(
    private val nmcliAL: INmcliAL,
    private val iwAL: IIwAL,
    private val reg: NetworkInterfaceRegistryV2,
    private val connService: WifiConnectionServiceV2,
    private val tetherServiceV2: WifiTetherServiceV2,
    private val sseManager: SseManager
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val ignoredTypes = setOf(NmCliDeviceType.Loopback, NmCliDeviceType.Wifi_p2p)

    // Cacher for å hindre duplikate meldinger ut på SSE
    private val lastConnectionCache = ConcurrentHashMap<String, WifiInterfaceClient>()
    private val lastTetherCache = ConcurrentHashMap<String, WifiInterfaceTether>()

    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {
        scan()
        startListening()
    }

    private fun startListening() {
        thread(start = true, name = "nmcli-monitor-thread") {
            val minIntervalMs = 4000L
            val lastScanTime = AtomicLong(0L)

            try {
                val process = ProcessBuilder("nmcli", "monitor").redirectErrorStream(true).start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (!line.isNullOrBlank()) {
                        val now = System.currentTimeMillis()
                        val last = lastScanTime.get()

                        if (now - last > minIntervalMs) {
                            if (lastScanTime.compareAndSet(last, now)) {
                                log.debug("Nmcli monitor event trigget ny scan og state-synk: $line")
                                scan()
                            }
                        } else {
                            log.trace("Nmcli monitor event ignorert (throttlet): $line")
                        }
                    }
                }
            } catch (e: Exception) {
                log.error("Feil i nmcli monitor-lytter", e)
            }
        }
    }

    @Scheduled(fixedRate = 30000)
    fun scan() {
        //log.info("Scanning network interface og synkroniserer state")
        val devices = nmcliAL.getDevices()

        devices
            .filter { it.ifType !in ignoredTypes }
            .forEach { device ->
                val ifName = device.ifName
                val mac = nmcliAL.getDeviceHWADDR(ifName) ?: "unknown"

                val connName = nmcliAL.getConnectionName(ifName)
                val wirelessMode = connName?.let { nmcliAL.getWirelessMode(it) }

                val mode = when {
                    device.isExternal -> NetworkInterfaceMode.External
                    wirelessMode == InterfaceMode.Tether -> NetworkInterfaceMode.Tether
                    wirelessMode == InterfaceMode.Client -> NetworkInterfaceMode.Client
                    device.state.org == "connected" -> NetworkInterfaceMode.Client
                    else -> NetworkInterfaceMode.Idle
                }

                val networkInterface = when (device.ifType) {
                    NmCliDeviceType.Wifi -> {
                        val phy = iwAL.getPhysicalInterfaces(ifName)
                        val caps = phy?.let { iwAL.getWirelessCapabilities(it) } ?: emptySet()

                        WirelessNetworkInterface(
                            interfaceName = ifName,
                            macAdress = mac,
                            mode = mode,
                            caps = caps
                        )
                    }
                    NmCliDeviceType.Ethernet -> {
                        EthernetNetworkInterface(
                            interfaceName = ifName,
                            macAdress = mac,
                            mode = mode
                        )
                    }
                    else -> null
                }

                if (networkInterface != null) {
                    reg.registerOrUpdate(networkInterface)
                    updateOrSet(networkInterface.interfaceName)
                }
            }
    }

    fun updateOrSet(ifaceName: String) {
        val connectionStates = connService.getCurrentState()
        val tetherStates = tetherServiceV2.getCurrentState()

        // --- 1. Sjekk Client / Connection ---
        val currentClient = connectionStates.find { it.name == ifaceName }
        val cachedClient = lastConnectionCache[ifaceName]

        if (currentClient != cachedClient) {
            if (currentClient != null) {
                lastConnectionCache[ifaceName] = currentClient
            } else {
                lastConnectionCache.remove(ifaceName)
            }

            sseManager.send(SSEWifiInterfaceClient(connectionStates))

            // Suppress: Kun send individuell event hvis vi er Connected OG har et gyldig nettverk
            val payload = currentClient?.let { WifiConnection(it.name, it.state, it.network) }
            val isConnectedWithNetwork = payload?.state == WifiConnectionStateType.Connected && payload.network != null

            // Hvis det ikke er Connected, eller hvis det er Connected men mangler nettverk,
            // så sender vi den likevel dersom tilstanden har endret seg til f.eks Disconnected.
            // Men vi unngår "tomme" connected-events.
            if (payload?.state != WifiConnectionStateType.Connected || isConnectedWithNetwork) {
                sseManager.send(SSEWifiConnection(ifaceName, payload))
            }
        }

        // --- 2. Sjekk Tether ---
        val currentTether = tetherStates.find { it.name == ifaceName }
        val cachedTether = lastTetherCache[ifaceName]

        if (currentTether != cachedTether) {
            if (currentTether != null) {
                lastTetherCache[ifaceName] = currentTether
            } else {
                lastTetherCache.remove(ifaceName)
            }

            sseManager.send(SSEWifiInterfaceTether(tetherStates))

            // Suppress: Samme logikk for Tether, kun send payload hvis vi faktisk tetherer (og har nett)
            // eller hvis vi har gått tilbake til Idle/Error.
            val payload = currentTether?.let { WifiTether(it.name, it.state, it.network) }
            val isTetheringWithNetwork = payload?.state == WirelessTetheringState.Tethering && payload.network != null

            if (payload?.state != WirelessTetheringState.Tethering || isTetheringWithNetwork) {
                sseManager.send(SSEWifiTether(ifaceName, payload))
            }
        }
    }
}