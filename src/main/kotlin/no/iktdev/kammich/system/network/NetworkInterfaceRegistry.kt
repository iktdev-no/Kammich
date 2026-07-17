package no.iktdev.kammich.system.network

import no.iktdev.kammich.models.shared.network.NetworkInterface
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode
import no.iktdev.kammich.models.shared.network.NetworkInterfaceType
import no.iktdev.kammich.system.network.wifi.WifiTetherService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList

@Component
class NetworkInterfaceRegistry {

    private val log = LoggerFactory.getLogger(javaClass)

    private val registry = CopyOnWriteArrayList<NetworkInterface>()

    // En "Lease" som gir deg tilgang til interfacet
    data class InterfaceLease(
        val interfaceName: String,
        val assignedMode: NetworkInterfaceMode,
        private val releaseAction: (String) -> Unit
    ) {
        // Brukeren av leasen må kalle release når de er ferdige
        fun release() = releaseAction(interfaceName)
    }

    // Inne i NetworkRegistry.kt
    private fun isModeAllowed(type: NetworkInterfaceType, mode: NetworkInterfaceMode): Boolean {
        return when (mode) {
            else -> true
            // Her legger du til flere etter hvert som vi definerte i samtalen vår
        }
    }

    // Spør om å få en rolle på et interface
    fun obtain(interfaceName: String, requestedMode: NetworkInterfaceMode): InterfaceLease? {
        synchronized(registry) {
            val iface = registry.find { it.interfaceName == interfaceName } ?: return null

            // Sjekk om den er ledig og om rollen er lovlig for typen
            if (iface.mode != NetworkInterfaceMode.Idle) return null
            if (!isModeAllowed(iface.type, requestedMode)) return null

            // Oppdater tilstanden internt
            val updated = iface.withMode(newMode = requestedMode)
            registry.remove(iface)
            registry.add(updated)

            // Returner "nøkkelen" til å bruke interfacet
            return InterfaceLease(interfaceName, requestedMode) { name ->
                release(name)
            }
        }
    }

    private fun release(interfaceName: String) {
        synchronized(registry) {
            val iface = registry.find { it.interfaceName == interfaceName } ?: return
            registry.remove(iface)
            registry.add(iface.withMode(newMode = NetworkInterfaceMode.Idle))
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
}