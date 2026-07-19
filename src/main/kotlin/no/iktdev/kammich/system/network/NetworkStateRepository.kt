@file:OptIn(ExperimentalAtomicApi::class)

package no.iktdev.kammich.system.network

import no.iktdev.kammich.models.internal.network.*
import no.iktdev.kammich.models.shared.network.InterfaceActiveState
import no.iktdev.kammich.models.shared.network.WifiNetworkConnection
import no.iktdev.kammich.models.shared.network.WifiNetworkTether
import no.iktdev.kammich.models.shared.network.WirelessNetworkInterface
import no.iktdev.kammich.models.shared.network.WirelessTetheringState
import org.springframework.stereotype.Component
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.updateAndFetch

@Component
class NetworkStateRepository {
    private val state = AtomicReference(NetworkState())

    fun getCurrentState(): NetworkState = state.load()

    /**
     * Oppdaterer tilstanden til et spesifikt interface.
     * Hvis interfacet ikke finnes, sendes 'null' til transform-funksjonen.
     */
    fun updateInterface(name: String, transform: (InterfaceState?) -> InterfaceState): InterfaceState {
        var result: InterfaceState? = null
        state.updateAndFetch { current ->
            val updated = transform(current.interfaces[name])
            result = updated // Fang opp den nye staten
            current.copy(interfaces = current.interfaces + (name to updated))
        }
        return result!!
    }

    /**
     * Oppdaterer global Tailscale-tilstand.
     */
    fun updateTailscale(transform: (TailscaleState) -> TailscaleState) {
        state.updateAndFetch { current ->
            TODO("Not implemented yet")
        }
    }

    /**
     * Sletter et interface fra listen (f.eks. ved deaktivering/fjerning).
     */
    fun removeInterface(name: String) {
        state.updateAndFetch { current ->
            current.copy(interfaces = current.interfaces - name)
        }
    }

    fun updateInterface(res: InterfaceResolution, nif: WirelessNetworkInterface) {

        state.updateAndFetch { current ->
            val updatedState = when (val obj = res.stateObject) {
                is WifiNetworkTether -> {
                    val ias =
                        if (obj.state == WirelessTetheringState.Broadcasting) InterfaceActiveState.Tethering else InterfaceActiveState.Idle
                    current.interfaces[nif.interfaceName]?.asWifi()?.setTethering(
                        ias, obj
                    ) ?: WifiInterfaceState(
                        mac = nif.macAdress,
                        mode = nif.mode,
                        tethering = obj,
                        state = ias,
                    )
                }

                is WifiNetworkConnection -> {
                    current.interfaces[nif.interfaceName]?.asWifi()?.setNetwork(
                        obj.state, obj.network
                    ) ?: WifiInterfaceState(
                        mac = nif.macAdress,
                        mode = nif.mode,
                        network = obj.network,
                        state = obj.state,
                    )
                }

                else -> null
            }

            if (updatedState != null) {
                current.copy(interfaces = current.interfaces + (nif.interfaceName to updatedState))
            } else current
        }
    }

    fun updateInterface(state: InterfaceResolution, nif: EthernetInterfaceState) {

    }
}