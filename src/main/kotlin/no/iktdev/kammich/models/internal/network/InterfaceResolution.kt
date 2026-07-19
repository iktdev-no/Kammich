package no.iktdev.kammich.models.internal.network

import no.iktdev.kammich.models.shared.network.InterfaceActiveState
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode
import no.iktdev.kammich.models.shared.network.WifiNetworkInterface

data class InterfaceResolution(
    val mode: NetworkInterfaceMode,
    val activeState: InterfaceActiveState,
    val stateObject: WifiNetworkInterface? = null
)