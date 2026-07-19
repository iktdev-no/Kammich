package no.iktdev.kammich.models.internal.network

import no.iktdev.kammich.models.shared.network.InterfaceActiveState
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiNetworkTether
import no.iktdev.kammich.models.shared.network.WirelessConnection
import no.iktdev.kammich.models.shared.network.WirelessNetworkSearch
import no.iktdev.kammich.models.shared.network.WirelessOperatingState
import no.iktdev.kammich.models.shared.network.WirelessTethering
import no.iktdev.kammich.models.shared.network.WirelessTetheringState
import java.time.ZonedDateTime

data class NetworkState(
    // Map fra interface-navn (f.eks. "wlp1s0") til dens tilstand
    val interfaces: Map<String, InterfaceState> = emptyMap(),
    val tailscaleState: TailscaleState? = null
)

sealed interface InterfaceState {
    val mac: String
    val mode: NetworkInterfaceMode
    val type: InterfaceType
    val state: InterfaceActiveState

}

fun InterfaceState.setMode(newMode: NetworkInterfaceMode): InterfaceState {
    return when (this) {
        is WifiInterfaceState -> this.copy(
            mode = newMode,
            state = InterfaceActiveState.Idle, // Reset state ved mode-bytte
            network = null,
            scan = null                        // Reset scan
        )
        is EthernetInterfaceState -> this.copy(mode = newMode)
    }
}

fun InterfaceState.setState(newState: InterfaceActiveState): InterfaceState {
    return when (this) {
        is WifiInterfaceState -> {
            if (this.state !in listOf(InterfaceActiveState.Connecting, InterfaceActiveState.Connected)) {
                this.copy(state = newState)
            } else this
        }
        is EthernetInterfaceState -> this.copy(state = newState)
    }
}

// Spesifikk for Wifi
fun InterfaceState.asWifi(): WifiInterfaceState? = this as? WifiInterfaceState

fun InterfaceState.setScan(state: InterfaceActiveState, newScan: WifiScanState): InterfaceState {
    val useState = if (this.state !in listOf(InterfaceActiveState.Connecting, InterfaceActiveState.Connected)) state else this.state
    return (this as? WifiInterfaceState)?.copy(state = useState, scan = newScan) ?: this
}

fun InterfaceState.setNetwork(state: InterfaceActiveState, network: WifiNetwork?): InterfaceState {
    return (this as? WifiInterfaceState)?.copy(state = state, network = network) ?: this
}

fun InterfaceState.setTethering(state: InterfaceActiveState, tethering: WifiNetworkTether?): InterfaceState {
    return (this as? WifiInterfaceState)?.copy(state = state, tethering = tethering) ?: this
}

data class WifiInterfaceState(
    override val mac: String,
    override val mode: NetworkInterfaceMode = NetworkInterfaceMode.Idle,
    override val state: InterfaceActiveState = InterfaceActiveState.Idle,
    val network: WifiNetwork? = null,
    val scan: WifiScanState? = null,
    val tethering: WifiNetworkTether? = null // Kun for Master-interfaces
): InterfaceState {
    override val type = InterfaceType.Wifi
    fun toWirelessOperatingState(): WirelessOperatingState {
        return when (mode) {
            NetworkInterfaceMode.Master -> WirelessOperatingState.AP
            NetworkInterfaceMode.Client, NetworkInterfaceMode.External -> WirelessOperatingState.STA
            else -> WirelessOperatingState.Idle
        }
    }
    fun scanToWirelessNetworkSearch(): WirelessNetworkSearch? {
        return scan?.let {
            WirelessNetworkSearch(
                networks = it.networks,
                lastSearched = it.performedAt.toString()
            )
        }
    }
    fun connectionToWirelessConnection(): WirelessConnection {
        val useState = when (state) {
            InterfaceActiveState.Connecting -> InterfaceActiveState.Connecting
            InterfaceActiveState.Connected -> InterfaceActiveState.Connected
            InterfaceActiveState.Disconnected -> InterfaceActiveState.Disconnected
            else -> InterfaceActiveState.Idle
        }
        return WirelessConnection(
            network = network,
            state = useState
        )
    }
    fun tetheringToWirelessTethering(): WirelessTethering {
        val useState = when (state) {
            InterfaceActiveState.Tethering -> WirelessTetheringState.Broadcasting
            else -> WirelessTetheringState.Idle
        }
        return WirelessTethering(
            network = network,
            state = useState
        )
    }
}

data class EthernetInterfaceState(
    override val mac: String,
    override val mode: NetworkInterfaceMode = NetworkInterfaceMode.Idle,
    override val state: InterfaceActiveState = InterfaceActiveState.Idle,
    ): InterfaceState {
    override val type = InterfaceType.Ethernet
}

data class WifiScanState(
    val networks: List<WifiNetwork> = emptyList(),
    val performedAt: ZonedDateTime = ZonedDateTime.now()
)

data class TailscaleState(
    val name: String,
    val isRunning: Boolean = false,
    val ipAddress: String? = null,
    val loginUrl: String? = null
)


enum class InterfaceType {
    Wifi,
    Ethernet
}