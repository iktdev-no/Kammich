package no.iktdev.kammich.system.network

import no.iktdev.kammich.models.internal.network.NmCliDevice
import no.iktdev.kammich.models.internal.network.NmCliDeviceType
import no.iktdev.kammich.models.shared.network.*
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.networking.SSEEthernetConnection
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
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Service
class NetworkInterfaceScannerV2(
    private val nmcliAL: INmcliAL,
    private val iwAL: IIwAL,
    private val reg: NetworkInterfaceRegistryV2,
    private val wifiConn: WifiConnectionServiceV2,
    private val wifiTether: WifiTetherServiceV2,
    private val ethernetService: EthernetConnectionService,
    private val sseManager: SseManager
) : NmcliMonitor.NmcliMonitorListener {
    private val log = LoggerFactory.getLogger(javaClass)
    private val ignoredTypes = setOf(NmCliDeviceType.Loopback, NmCliDeviceType.Wifi_p2p)

    private val lastScan = AtomicReference<Instant?>(null)
    private val scanRunning = AtomicBoolean(false)

    companion object {
        private const val SCAN_INTERVAL_SECONDS = 30L
        private const val ALIVE_TIMEOUT_SECONDS = SCAN_INTERVAL_SECONDS * 2
    }

    fun isAlive(): Boolean {
        val last = lastScan.load() ?: return false

        return Duration.between(last, Instant.now()).seconds <
                ALIVE_TIMEOUT_SECONDS
    }

    fun getLastScan(): Instant? = lastScan.load()

    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {
        requestScan()
    }

    override fun onNetworkManagerEvent(event: String) {
        log.info("NetworkManager event: {}", event)
        requestScan()
    }

    @Scheduled(
        fixedRate = SCAN_INTERVAL_SECONDS * 1000,
        initialDelay = 10_000
    )
    fun scheduledScan() {
        requestScan()
    }

    private fun requestScan() {
        if (!scanRunning.compareAndSet(false, true)) {
            log.info("Blocking scan, due to already running")
            return
        }

        try {
            lastScan.store(Instant.now())
            scan()
        } finally {
            scanRunning.set(false)
        }
    }

    fun scan() {
        log.debug("Scanning for network interface")
        val devices = nmcliAL.getDevices()
            .filter { it.ifType !in ignoredTypes }

        val wifiInterfaces = devices.filter { it.ifType == NmCliDeviceType.Wifi }
            .map { handleWifiInterfaces(it) }
            .onEach {
                reg.registerOrUpdate(it)
                updateOrSetWifi(it)
            }

        val ethInterfaces = devices.filter { it.ifType == NmCliDeviceType.Ethernet }
            .map { handleEthernetInterfaces(it) }
            .onEach {
                reg.registerOrUpdate(it)
                updateOrSetEthernet(it)
            }
    }

    private fun handleEthernetInterfaces(iface: NmCliDevice): EthernetNetworkInterface {
        val ifName = iface.ifName
        val mac = nmcliAL.getDeviceHWADDR(ifName) ?: "unknown"

        val connName = nmcliAL.getConnectionName(ifName)
        val ethernetMode = connName?.let { nmcliAL.getEthernetMode(it) }

        val mode = when {
            iface.isExternal -> NetworkInterfaceMode.External
            ethernetMode == InterfaceMode.Tether -> NetworkInterfaceMode.Tether
            ethernetMode == InterfaceMode.Client -> NetworkInterfaceMode.Client
            iface.state.org == "connected" -> NetworkInterfaceMode.Client
            else -> NetworkInterfaceMode.Idle
        }

        return EthernetNetworkInterface(
            interfaceName = ifName,
            macAdress = mac,
            mode = mode
        )
    }

    private fun handleWifiInterfaces(iface: NmCliDevice): WirelessNetworkInterface {
        val ifName = iface.ifName
        val mac = nmcliAL.getDeviceHWADDR(ifName) ?: "unknown"

        val connName = nmcliAL.getConnectionName(ifName)
        val wirelessMode = connName?.let { nmcliAL.getWirelessMode(it) }

        val mode = when {
            iface.isExternal -> NetworkInterfaceMode.External
            wirelessMode == InterfaceMode.Tether -> NetworkInterfaceMode.Tether
            wirelessMode == InterfaceMode.Client -> NetworkInterfaceMode.Client
            iface.state.org == "connected" -> NetworkInterfaceMode.Client
            else -> NetworkInterfaceMode.Idle
        }

        val phy = iwAL.getPhysicalInterfaces(ifName)
        val caps = phy?.let { iwAL.getWirelessCapabilities(it) } ?: emptySet()

        return WirelessNetworkInterface(
            interfaceName = ifName,
            macAdress = mac,
            mode = mode,
            caps = caps
        )
    }

    private fun updateOrSetWifi(iface: WirelessNetworkInterface) {
        val ifaceName = iface.interfaceName

        when (iface.mode) {
            NetworkInterfaceMode.Client -> {
                val states = wifiConn.getCurrentState()
                val current = states.find { it.name == ifaceName }

                sseManager.send(SSEWifiInterfaceClient(states))

                val payload = current?.let {
                    WifiConnection(it.name, it.state, it.network)
                }

                if (payload?.state != WifiConnectionStateType.Connected ||
                    payload.network != null
                ) {
                    sseManager.send(SSEWifiConnection(ifaceName, payload))
                }
            }

            NetworkInterfaceMode.Tether -> {
                val states = wifiTether.getCurrentState()
                val current = states.find { it.name == ifaceName }

                sseManager.send(SSEWifiInterfaceTether(states))

                val payload = current?.let {
                    WifiTether(it.name, it.state, it.network)
                }

                if (payload?.state != WirelessTetheringState.Tethering ||
                    payload.network != null
                ) {
                    sseManager.send(SSEWifiTether(ifaceName, payload))
                }
            }

            else -> Unit
        }
    }

    private fun updateOrSetEthernet(iface: EthernetNetworkInterface) {
        ethernetService.evaluateInterface(iface.interfaceName)
        ethernetService.getConnection(iface.interfaceName)?.let {
            sseManager.send(SSEEthernetConnection(iface.interfaceName, it))
        }
    }
}