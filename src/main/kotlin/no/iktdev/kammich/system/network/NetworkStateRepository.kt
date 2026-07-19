@file:OptIn(ExperimentalAtomicApi::class)

package no.iktdev.kammich.system.network

import no.iktdev.kammich.models.internal.network.*
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
}