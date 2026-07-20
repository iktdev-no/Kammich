package no.iktdev.kammich.system.network

import no.iktdev.kammich.models.internal.network.EthernetInterfaceState
import no.iktdev.kammich.models.shared.network.InterfaceActiveState
import no.iktdev.kammich.models.internal.network.InterfaceAvailability
import no.iktdev.kammich.models.internal.network.InterfaceState
import no.iktdev.kammich.models.internal.network.WifiInterfaceState
import no.iktdev.kammich.models.internal.network.setMode
import no.iktdev.kammich.models.internal.network.setState
import no.iktdev.kammich.models.shared.network.EthernetNetworkInterface
import no.iktdev.kammich.models.shared.network.NetworkInterface
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode
import no.iktdev.kammich.models.shared.network.NetworkInterfaceType
import no.iktdev.kammich.models.shared.network.WirelessNetworkInterface
import no.iktdev.kammich.models.shared.network.WirelessNetworkInterfaceCapability
import no.iktdev.kammich.models.shared.network.asWifi
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList

@Component
class NetworkInterfaceRegistry(
    private val repository: NetworkStateRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val registry = CopyOnWriteArrayList<NetworkInterface>()

    fun listNetworkInterfaces(): List<NetworkInterface> {
        return registry.toList()
    }

    // En "Lease" som gir deg tilgang til interfacet
    data class InterfaceLease(
        private val nif: NetworkInterface,
        val mode: NetworkInterfaceMode,
        private val repository: NetworkStateRepository, // Inject repository her
        private val releaseAction: (String) -> Unit
    ) {
        fun getInterfaceName() = nif.interfaceName

        // Brukeren av leasen må kalle release når de er ferdige
        fun release() = releaseAction(nif.interfaceName)

        fun getState() = repository.getCurrentState().interfaces[nif.interfaceName]

        /**
         * Brukes for å endre KONFIGURASJON (Client/Master).
         * Dette utløser en hard reset av state (til Idle).
         */
        fun setMode(newMode: NetworkInterfaceMode, onUpdated: (InterfaceState) -> Unit) {
            update({ it.setMode(newMode) }, onUpdated)
        }

        /**
         * Brukes for å endre RUNTIME-STATUS (Scanning/Connected).
         * Dette endrer bare 'state'-feltet, ikke 'mode'.
         */
        fun setState(newState: InterfaceActiveState, onUpdated: (InterfaceState) -> Unit) {
            update({ it.setState(newState) }, onUpdated)
        }

        // Basismetoden for all state-manipulasjon
        fun update(
            transform: (InterfaceState) -> InterfaceState,
            onUpdated: (InterfaceState) -> Unit
        ) {
            val newState = repository.updateInterface(nif.interfaceName) { existing ->
                val stateToTransform = existing ?: createInitialStateFromNif()
                transform(stateToTransform)
            }
            onUpdated(newState)
        }

        private fun createInitialStateFromNif(): InterfaceState {
            return when (nif) {
                is WirelessNetworkInterface -> WifiInterfaceState(mac = nif.macAdress, mode = nif.mode)
                is EthernetNetworkInterface -> EthernetInterfaceState(mac = nif.macAdress, mode = nif.mode)
                else -> throw IllegalArgumentException("Unknown network interface")
            }
        }
    }

    private fun isModeAllowed(iface: NetworkInterface, mode: NetworkInterfaceMode): Boolean {
        return when (mode) {

            else -> true
            // Her legger du til flere etter hvert som vi definerte i samtalen vår
        }
    }

    // Spør om å få en rolle på et interface
    fun <T> obtain(
        interfaceName: String,
        requestedMode: NetworkInterfaceMode,
        onReject: () -> Unit,
        block: (InterfaceLease) -> T
    ): T? {
        synchronized(registry) {
            val iface = registry.find { it.interfaceName == interfaceName }

            // Vi sjekker nå at vi enten er Idle ELLER at vi ber om samme modus igjen (re-obtain)
            if (iface == null || (iface.mode != NetworkInterfaceMode.Idle && iface.mode != requestedMode)) {
                log.info("$iface is in ${iface?.mode?.name} which is not compatible with ${requestedMode.name}")
                onReject()
                return null
            }

            if (!isModeAllowed(iface, requestedMode)) {
                onReject()
                return null
            }

            // Oppdater hvis vi var Idle
            if (iface.mode == NetworkInterfaceMode.Idle) {
                registry.remove(iface)
                registry.add(iface.withMode(requestedMode))
            }

            val lease = InterfaceLease(iface, requestedMode, repository) { release(it) }

            return try {
                block(lease)
            } catch (e: Exception) {
                log.error("Konfigurasjon feilet for $interfaceName, frigjør lease", e)
                // Siden du har tilgang til lease.release() inni blokka,
                // kan du kalle den her hvis det skjærer seg
                if (requestedMode == iface.mode) {
                    lease.release()
                }
                null
            }
            // Vi lar interfacet stå i requestedMode etter at blokka er ferdig!
        }
    }

    private fun release(interfaceName: String) {
        synchronized(registry) {
            val iface = registry.find { it.interfaceName == interfaceName } ?: return
            val updated = iface.withMode(NetworkInterfaceMode.Idle)
            registry.remove(iface)
            registry.add(updated)
            log.info("Interface $interfaceName er nå frigjort til Idle")
        }
    }

    // 3. BORROW: Låner interfacet kun i hele blokkas levetid (f.eks. for scanning)
    fun borrow(
        interfaceName: String,
        temporaryMode: NetworkInterfaceMode,
        onReject: () -> Unit,
        block: (NetworkInterface) -> Unit
    ) {
        synchronized(registry) {
            val iface = registry.find { it.interfaceName == interfaceName }
            if (iface == null || iface.mode != NetworkInterfaceMode.Idle) {
                onReject()
                return
            }

            val updated = iface.withMode(temporaryMode)
            registry.remove(iface)
            registry.add(updated)

            try {
                block(updated)
            } finally {
                // Uansett hva som skjer, blir den NULLSTILT til Idle når blokka dør!
                synchronized(registry) {
                    registry.remove(updated)
                    registry.add(iface.withMode(NetworkInterfaceMode.Idle))
                }
            }
        }
    }

    // Inne i NetworkInterfaceRegistry
    fun registerOrUpdate(iface: NetworkInterface) {
        synchronized(registry) {
            val existing = registry.find { it.interfaceName == iface.interfaceName }
            if (existing != null) {
                // Oppdater bare hvis noe har endret seg (f.eks. tilstand)
                registry.remove(existing)
            } else {
                log.info("A new network interface is being registered ${iface.interfaceName} (${iface.type})")
            }
            registry.add(iface)
        }
    }

    fun findInterface(name: String? = null, mac: String? = null): NetworkInterface? {
        return registry.find { iface ->
            (name != null && iface.interfaceName == name) ||
                    (mac != null && iface.macAdress.equals(mac, ignoreCase = true))
        }
    }

    fun getInterfaces(type: NetworkInterfaceType, desiredModes: NetworkInterfaceMode) = getInterfaces(type, setOf(desiredModes))

    fun getInterfaces(type: NetworkInterfaceType, desiredModes: Set<NetworkInterfaceMode>): List<InterfaceAvailability> {
        return registry.filter { it.type == type }.map { nif ->
            val state = repository.getCurrentState().interfaces[nif.interfaceName]
            val currentMode = state?.mode ?: nif.mode

            // Definer hva som er "kompatibelt" med desiredModes
            val isModeCompatible = currentMode in desiredModes
            val isIdle = currentMode == NetworkInterfaceMode.Idle

            // Hovedregel:
            // 1. Hvis den er i ønsket modus, er den tilgjengelig.
            // 2. Hvis den er Idle, er den tilgjengelig.
            // 3. Hvis den er Master og vi ber om Client, er den IKKE tilgjengelig.

            val isAvailable = when {
                // Hvis vi ber om Master, sjekk også hardware-støtte
                desiredModes.contains(NetworkInterfaceMode.Master) -> {
                    val supportsAp = nif.asWifi()?.caps?.any {
                        it in listOf(WirelessNetworkInterfaceCapability.AP, WirelessNetworkInterfaceCapability.Concurrent)
                    } ?: false

                    supportsAp && (isIdle || isModeCompatible)
                }

                // Hvis vi ber om Client (eller annet), sjekk kun modus
                else -> isIdle || isModeCompatible
            }

            // Logging for å debugge hvorfor den feiler
            if (!isAvailable) {
                log.info("${nif.interfaceName} is in $currentMode, returning isAvailable false for request which states: $desiredModes")
            }

            InterfaceAvailability(
                nif = nif,
                state = state,
                isAvailable = isAvailable
            )
        }
    }


}