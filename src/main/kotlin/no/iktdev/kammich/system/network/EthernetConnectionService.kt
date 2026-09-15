package no.iktdev.kammich.system.network

import jakarta.annotation.PostConstruct
import no.iktdev.kammich.models.internal.network.NmCliDeviceState
import no.iktdev.kammich.models.shared.network.EthernetConnection
import no.iktdev.kammich.models.shared.network.EthernetConnectionStateType
import no.iktdev.kammich.models.shared.network.EthernetNetworkInterface
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.networking.SSEEthernetConnection
import no.iktdev.kammich.system.network.strategy.lan.EthernetModeStrategy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Service
class EthernetConnectionService(
    private val sseManager: SseManager,
    private val interfaceRegistry: NetworkInterfaceRegistryV2,
    private val strategies: List<EthernetModeStrategy>,
    private val captivePortal: CaptivePortal,
) : NmcliMonitor.NmcliMonitorListener {
    private val log = LoggerFactory.getLogger(javaClass)

    private val clientTimeoutTasks = ConcurrentHashMap<String, ScheduledFuture<*>>()
    private val clientTimeoutAt = ConcurrentHashMap<String, Instant>()
    private val emergencyAllowed = ConcurrentHashMap<String, Boolean>()
    private val scheduler = Executors.newScheduledThreadPool(2)

    companion object {
        private const val CLIENT_TIMEOUT_SECONDS = 90L
    }

    private fun getActiveStrategy(): EthernetModeStrategy? =
        strategies.find { it.isSupported() }

    fun getCurrentState(interfaceName: String): EthernetConnectionStateType {
        val iface = interfaceRegistry.findInterface(name = interfaceName)
            ?: return EthernetConnectionStateType.Disconnected

        val strategy = getActiveStrategy() ?: return EthernetConnectionStateType.Disconnected
        val carrier = strategy.hasCarrier(interfaceName)

        if (carrier == false) return EthernetConnectionStateType.Disconnected

        return when (iface.mode) {
            NetworkInterfaceMode.Client ->
                if (!strategy.getDeviceIpv4Addresses(interfaceName).isNullOrBlank())
                    EthernetConnectionStateType.Connected
                else
                    EthernetConnectionStateType.Connecting

            NetworkInterfaceMode.Tether ->
                EthernetConnectionStateType.Emergency

            else ->
                EthernetConnectionStateType.Disconnected
        }
    }

    fun getConnection(interfaceName: String): EthernetConnection? {
        val iface = interfaceRegistry.findInterface(name = interfaceName) ?: return null
        val strategy = getActiveStrategy() ?: return null

        return EthernetConnection(
            ifName = interfaceName,
            state = getCurrentState(interfaceName),
            carrier = strategy.hasCarrier(interfaceName) ?: false,
            deviceState = strategy.getDeviceState(interfaceName),
            ipv4 = strategy.getDeviceIpv4Addresses(interfaceName)
                ?: strategy.getProfileIpv4Address(interfaceName),
            emergencyAllowed = emergencyAllowed[interfaceName] ?: true,
            emergencyAt = clientTimeoutAt[interfaceName],
            error = null
        )
    }

    fun getConnections(): List<EthernetConnection> =
        interfaceRegistry.listNetworkInterfaces()
            .filterIsInstance<EthernetNetworkInterface>()
            .mapNotNull { getConnection(it.interfaceName) }

    override fun onNetworkManagerEvent(event: String) {
        log.debug("Ethernet received NetworkManager event: {}", event)

        val interfaceName = event.substringBefore(":").trim()
        if (interfaceName.isBlank()) return


        val iface = interfaceRegistry.findInterface(name = interfaceName) ?: return
        if (iface !is EthernetNetworkInterface) return

        val strategy = getActiveStrategy() ?: return

        scheduler.execute {
            evaluateInterface(iface.interfaceName, iface.mode, strategy)
        }
    }

    fun evaluateInterface(interfaceName: String) {
        val iface = interfaceRegistry.findInterface(name = interfaceName) as? EthernetNetworkInterface ?: return
        val strategy = getActiveStrategy() ?: return

        scheduler.execute {
            evaluateInterface(iface.interfaceName, iface.mode, strategy)
        }
    }
    private fun evaluateInterface(
        interfaceName: String,
        mode: NetworkInterfaceMode,
        strategy: EthernetModeStrategy
    ) {
        val deviceState = strategy.getDeviceState(interfaceName)
        val carrier = strategy.hasCarrier(interfaceName)

        log.debug(
            "Evaluating Ethernet {}: mode={}, nmState={}, carrier={}",
            interfaceName,
            mode,
            deviceState,
            carrier
        )

        if (!carrier) {
            disconnect(interfaceName)
            return
        }

        when (mode) {
            NetworkInterfaceMode.Client -> {
                if (!strategy.getDeviceIpv4Addresses(interfaceName).isNullOrBlank()) {
                    cancelClientTimeout(interfaceName)
                } else if (!clientTimeoutTasks.containsKey(interfaceName)) {
                    log.warn("Ethernet {} is in Client without an active timeout, resetting Client state", interfaceName)
                    interfaceRegistry.forceReleaseAll(interfaceName)
                    startClient(interfaceName)
                }
            }

            NetworkInterfaceMode.Tether ->
                cancelClientTimeout(interfaceName)

            NetworkInterfaceMode.Idle -> startClient(interfaceName)

            else -> Unit
        }
    }

    fun setEmergencyAllowed(interfaceName: String, allowed: Boolean): Boolean {
        if (interfaceRegistry.findInterface(name = interfaceName) !is EthernetNetworkInterface)
            return false

        emergencyAllowed[interfaceName] = allowed
        getConnection(interfaceName)?.let {
            sseManager.send(SSEEthernetConnection(interfaceName, it))
        }
        return true
    }

    fun isEmergencyAllowed(interfaceName: String): Boolean =
        emergencyAllowed[interfaceName] ?: true

    fun reset(interfaceName: String): Boolean {
        val strategy = getActiveStrategy() ?: return false

        return try {
            cancelClientTimeout(interfaceName)
            strategy.stopTetherMode(interfaceName)
            strategy.stopClientMode(interfaceName)
            interfaceRegistry.forceReleaseAll(interfaceName)
            startClient(interfaceName)
        } catch (e: Exception) {
            log.error("Failed to reset Ethernet $interfaceName", e)
            false
        }
    }

    fun startClient(interfaceName: String): Boolean {
        val strategy = getActiveStrategy() ?: return false
        return startClient(interfaceName, strategy)
    }

    private fun startClient(interfaceName: String, strategy: EthernetModeStrategy): Boolean {
        val lease = interfaceRegistry.acquire(
            interfaceName = interfaceName,
            requestedMode = NetworkInterfaceMode.Client,
            onReject = {
                log.debug("Could not acquire client lease for Ethernet $interfaceName")
                disconnect(interfaceName)
            }
        ) ?: return false
        getConnection(interfaceName)?.let {
            sseManager.send(SSEEthernetConnection(interfaceName, it))
        }

        val out = try {
            log.info("Starting Ethernet client mode on $interfaceName")

            if (!strategy.startClientMode(interfaceName)) {
                lease.release()
                return false
            }

            log.info(
                "Ethernet $interfaceName client mode started, waiting {}s for IPv4",
                CLIENT_TIMEOUT_SECONDS
            )

            scheduleClientTimeout(interfaceName, lease, strategy)
            true
        } catch (e: Exception) {
            lease.release()
            log.error("Failed to start Ethernet client mode on $interfaceName", e)
            false
        }
        getConnection(interfaceName)?.let {
            sseManager.send(SSEEthernetConnection(interfaceName, it))
        }
        return out
    }

    fun startTether(interfaceName: String): Boolean {
        val strategy = getActiveStrategy() ?: return false

        val lease = interfaceRegistry.acquire(
            interfaceName,
            NetworkInterfaceMode.Tether,
            onReject = {
                log.debug("Could not acquire emergency lease for Ethernet $interfaceName")
                disconnect(interfaceName)
            }
        ) ?: return false

        val out = try {
            strategy.startTetherMode(lease.interfaceName)
            true
        } catch (e: Exception) {
            lease.release()
            log.error("Could not start tether/emergency mode on ${lease.interfaceName}", e)
            false
        }
        getConnection(interfaceName)?.let {
            sseManager.send(SSEEthernetConnection(interfaceName, it))
        }
        return out
    }

    private fun scheduleClientTimeout(
        interfaceName: String,
        lease: NetworkInterfaceRegistryV2.InterfaceLease,
        strategy: EthernetModeStrategy
    ) {
        clientTimeoutTasks.remove(interfaceName)?.cancel(false)

        clientTimeoutAt[interfaceName] =
            Instant.now().plusSeconds(CLIENT_TIMEOUT_SECONDS)

        clientTimeoutTasks[interfaceName] = scheduler.schedule(
            {
                checkClientTimeout(interfaceName, lease, strategy)
            },
            CLIENT_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        )
    }

    private fun checkClientTimeout(
        interfaceName: String,
        lease: NetworkInterfaceRegistryV2.InterfaceLease,
        strategy: EthernetModeStrategy
    ) {
        clientTimeoutTasks.remove(interfaceName)
        clientTimeoutAt.remove(interfaceName)

        try {
            if (!strategy.getDeviceIpv4Addresses(interfaceName).isNullOrBlank()) {
                log.info("Ethernet $interfaceName received IPv4 address")
                return
            }

            if (!isEmergencyAllowed(interfaceName)) {
                log.warn("Ethernet $interfaceName client timeout reached, emergency mode is disabled")
                lease.release()
                return
            }

            log.warn(
                "Ethernet $interfaceName has no IPv4 after {} seconds, switching to tether",
                CLIENT_TIMEOUT_SECONDS
            )

            lease.release()
            startTether(interfaceName)
        } catch (e: Exception) {
            log.error("Failed handling Ethernet timeout for $interfaceName", e)
            lease.release()
        }
    }

    fun disconnect(interfaceName: String): Boolean {
        val strategy = getActiveStrategy() ?: return false
        val iface = interfaceRegistry.findInterface(name = interfaceName) ?: return true

        cancelClientTimeout(interfaceName)

        val onDisconnected: (NetworkInterfaceRegistryV2.InterfaceLease) -> Unit = {
            val carrier = strategy.hasCarrier(it.interfaceName) ?: false
            val ipv4 = strategy.getProfileIpv4Address(it.interfaceName)

            sseManager.send(
                SSEEthernetConnection(
                    it.interfaceName,
                    EthernetConnection(
                        ifName = it.interfaceName,
                        state = EthernetConnectionStateType.Disconnected,
                        carrier = carrier,
                        ipv4 = ipv4,
                        emergencyAllowed = isEmergencyAllowed(it.interfaceName),
                        emergencyAt = null
                    )
                )
            )
        }

        return try {
            when (iface.mode) {
                NetworkInterfaceMode.Client -> {
                    strategy.stopClientMode(interfaceName)
                    interfaceRegistry.releaseLease(interfaceName, NetworkInterfaceMode.Client, onDisconnected)
                }

                NetworkInterfaceMode.Tether -> {
                    strategy.stopTetherMode(interfaceName)
                    interfaceRegistry.releaseLease(interfaceName, NetworkInterfaceMode.Tether, onDisconnected)
                }

                else -> Unit
            }

            log.info("Ethernet $interfaceName disconnected")
            true
        } catch (e: Exception) {
            log.error("Failed to disconnect Ethernet $interfaceName", e)
            false
        }
    }

    private fun cancelClientTimeout(interfaceName: String) {
        clientTimeoutTasks.remove(interfaceName)?.cancel(false)
        clientTimeoutAt.remove(interfaceName)
    }
}