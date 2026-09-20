package no.iktdev.kammich.system.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import no.iktdev.kammich.immich.services.ImmichContextService
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode.Client
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode.External
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode.Tether
import no.iktdev.kammich.models.shared.network.NetworkInterfaceType
import no.iktdev.kammich.models.shared.network.WifiInterfaceErrorType
import no.iktdev.kammich.models.shared.network.WifiInterfaceState
import no.iktdev.kammich.models.shared.network.WifiInterfaceStateType
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.networking.SSEWifiState
import no.iktdev.kammich.system.network.strategy.state.WifiStateStrategy
import no.iktdev.kammich.system.network.wifi.WifiClient
import no.iktdev.kammich.system.network.wifi.WifiTether
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class WifiOperationService(
    private val sseManager: SseManager,
    private val strategies: List<WifiStateStrategy>,
    private val interfaceRegistry: NetworkInterfaceRegistryV2,
    private val wifiClient: WifiClient,
    private val wifiTether: WifiTether,
    private val captivePortal: CaptivePortal,
    private val immichContextService: ImmichContextService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private fun getActiveStrategy(): WifiStateStrategy? = strategies.find { it.isSupported() }


    fun getAll(): List<WifiInterfaceState> {
        return interfaceRegistry.getInterfaces(
            NetworkInterfaceType.Wifi,
            setOf(NetworkInterfaceMode.Client, NetworkInterfaceMode.Idle)
        ).mapNotNull { (nif, _) ->
            val ifName = nif.interfaceName
            getConnection(ifName)
        }
    }

    fun getConnection(interfaceName: String): WifiInterfaceState? {
        val strategy = getActiveStrategy()
            ?: return null

        val state = strategy.getState(interfaceName)

        if (state.state == WifiInterfaceStateType.Idle) {
            val leaseMode = interfaceRegistry.getActiveLeaseMode(interfaceName)

            if (leaseMode != null) {
                return state.copy(
                    state = WifiInterfaceStateType.Acquired,
                    operatingMode = leaseMode
                )
            }
        }

        return state
    }

    fun acquireDevice(interfaceName: String, mode: NetworkInterfaceMode): Boolean {
        val lease = interfaceRegistry.acquire(
            interfaceName = interfaceName,
            requestedMode = mode,
            onReject = {
                log.error("Could not obtain ClientDevice for $interfaceName")
            }
        ) ?: return false

        log.info("Acquired $mode lock on ${lease.interfaceName}")
        send(
            WifiInterfaceState(
                ifName = interfaceName,
                state = WifiInterfaceStateType.Acquired,
                operatingMode = NetworkInterfaceMode.Client
            )
        )
        return true
    }

    fun onReleaseLease(interfaceName: String) = scope.launch {
        val currentState = getConnection(interfaceName) ?: return@launch

        if (currentState.state != WifiInterfaceStateType.Acquired || currentState.operatingMode == NetworkInterfaceMode.Idle) {
            log.warn(
                "Cannot release lease on $interfaceName: " +
                        "state=${currentState.state}, " +
                        "mode=${currentState.operatingMode}"
            )
            return@launch
        }

        interfaceRegistry.releaseLease(
            interfaceName = interfaceName,
            mode = currentState.operatingMode
        ) {
            send(
                WifiInterfaceState(
                    ifName = interfaceName,
                    state = WifiInterfaceStateType.Idle,
                    operatingMode = NetworkInterfaceMode.Idle
                )
            )
        }
    }

    fun onConnectClient(
        interfaceName: String,
        bssid: String,
        password: String?
    ) = scope.launch {
        val currentState = getConnection(interfaceName) ?: return@launch
        send(currentState.copy(operatingMode = NetworkInterfaceMode.Client, state = WifiInterfaceStateType.Connecting))
        wifiClient.connect(
            ifName = interfaceName,
            bssid = bssid,
            password = password,
            listener = object : WifiClient.WifiClientConnectEvents {

                override fun onLeaseRejected() {
                    val state = getConnection(interfaceName) ?: return
                    send(state.copy(error = WifiInterfaceErrorType.Unknown))
                }

                override fun onNetworkNotFound() {
                    val state = getConnection(interfaceName) ?: return
                    send(state.copy(error = WifiInterfaceErrorType.ClientNetworkNotFound))
                }

                override fun onResult(state: WifiInterfaceState) {
                    send(state)

                    if (state.state == WifiInterfaceStateType.Connected) {
                        captivePortal.verify(interfaceName)
                        immichContextService.initializeAndVerifyContext()
                    }
                }

                override fun onError(error: Exception) {
                    val state = getConnection(interfaceName) ?: return

                    send(
                        state.copy(
                            state = WifiInterfaceStateType.Disconnected,
                            error = WifiInterfaceErrorType.Unknown
                        )
                    )
                }
            }
        )
    }

    fun onDisconnectClient(interfaceName: String) = scope.launch {
        val currentState = getConnection(interfaceName) ?: return@launch
        send(currentState.copy(state = WifiInterfaceStateType.Disconnecting))
        wifiClient.disconnect(
            ifName = interfaceName,
            listener = object : WifiClient.WifiClientDisconnectEvents {

                override fun onResult() {
                    send(
                        WifiInterfaceState(
                            ifName = interfaceName,
                            state = WifiInterfaceStateType.Disconnected,
                            operatingMode = NetworkInterfaceMode.Idle
                        )
                    )
                    immichContextService.initializeAndVerifyContext()
                }

                override fun onError(error: Exception) {
                    val state = getConnection(interfaceName) ?: return
                    send(state.copy(error = WifiInterfaceErrorType.Unknown))
                }
            }
        )
    }


    fun onStartTether(interfaceName: String) = scope.launch {
        val currentState = getConnection(interfaceName) ?: return@launch
        send(currentState.copy(operatingMode = NetworkInterfaceMode.Tether, state = WifiInterfaceStateType.Starting))

        wifiTether.start(
            ifName = interfaceName,
            listener = object : WifiTether.WifiTetherStartEvents {
                override fun onLeaseRejected() {
                    send(
                        WifiInterfaceState(
                            ifName = interfaceName,
                            state = WifiInterfaceStateType.Idle,
                            operatingMode = NetworkInterfaceMode.Tether,
                            error = WifiInterfaceErrorType.TetherDeviceNotFound
                        )
                    )
                }

                override fun onResult(state: WifiInterfaceState) {
                    send(state)
                }

                override fun onError(error: Exception) {
                    send(
                        WifiInterfaceState(
                            ifName = interfaceName,
                            state = WifiInterfaceStateType.Idle,
                            operatingMode = NetworkInterfaceMode.Tether,
                            error = WifiInterfaceErrorType.TetherStartFailed
                        )
                    )
                }
            }
        )
    }

    fun onStopTether(interfaceName: String) = scope.launch {
        val currentState = getConnection(interfaceName) ?: return@launch
        send(currentState.copy(operatingMode = NetworkInterfaceMode.Tether, state = WifiInterfaceStateType.Stopping))

        wifiTether.stop(
            ifName = interfaceName,
            listener = object : WifiTether.WifiTetherStopEvents {
                override fun onResult() {
                    send(
                        WifiInterfaceState(
                            ifName = interfaceName,
                            state = WifiInterfaceStateType.Idle,
                            operatingMode = NetworkInterfaceMode.Idle
                        )
                    )
                }

                override fun onError(error: Exception) {
                    val state = getConnection(interfaceName) ?: return

                    send(
                        state.copy(
                            error = WifiInterfaceErrorType.TetherStopFailed
                        )
                    )
                }
            }
        )
    }


    fun send(state: WifiInterfaceState) {
        sseManager.send(SSEWifiState(state.ifName, state))
    }

    fun reset(interfaceName: String) {
        val iface = interfaceRegistry.listNetworkInterfaces().find { it.interfaceName == interfaceName } ?: run {
            log.error("Could not find an interface: $interfaceName, for reset")
            return
        }
        when (iface.mode) {
            External, Client -> {
                onDisconnectClient(iface.interfaceName)
            }
            Tether -> {
                onStopTether(iface.interfaceName)
            }
            else -> {}
        }
        interfaceRegistry.forceReleaseAll(iface.interfaceName)
    }

}
