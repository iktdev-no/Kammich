package no.iktdev.kammich.system.network

import no.iktdev.kammich.models.internal.network.InterfaceAvailability
import no.iktdev.kammich.models.shared.network.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class NetworkInterfaceRegistryV2 {
    private val log = LoggerFactory.getLogger(javaClass)

    private val registry = ConcurrentHashMap<String, NetworkInterface>()

    // Holder styr på aktive leaser og antall referanser (brukere) per modus per grensesnitt
    private val activeLeases = ConcurrentHashMap<String, MutableMap<NetworkInterfaceMode, LeaseTracker>>()

    class LeaseTracker(
        val interfaceName: String,
        val mode: NetworkInterfaceMode,
        var refCount: Int = 0
    )

    inner class InterfaceLease(
        val interfaceName: String,
        val mode: NetworkInterfaceMode,
        private val releaseAction: (String, NetworkInterfaceMode) -> Unit,
        private val forceReleaseAction: (String) -> Unit
    ) : AutoCloseable {
        fun release() = releaseAction(interfaceName, mode)
        fun forceRelease() = forceReleaseAction(interfaceName)
        override fun close() = release()
    }

    fun listNetworkInterfaces(): List<NetworkInterface> = registry.values.toList()

    fun registerOrUpdate(iface: NetworkInterface) {
        registry.compute(iface.interfaceName) { _, existing ->
            if (existing == null) {
                log.info("[V2] Oppdaget og registrerer nytt grensesnitt: ${iface.interfaceName} (${iface.type})")
                iface
            } else {
                withMode(iface, existing.mode)
            }
        }
    }

    fun findInterface(name: String? = null, mac: String? = null): NetworkInterface? {
        return registry.values.find { iface ->
            (name != null && iface.interfaceName == name) ||
                    (mac != null && iface.macAdress.equals(mac, ignoreCase = true))
        }
    }

    /**
     * Skaff en lease. Hvis modusen allerede er aktiv, øker vi telleren (referansen)
     * i stedet for å overskrive eller resette enheten.
     */
    fun acquire(
        interfaceName: String,
        requestedMode: NetworkInterfaceMode,
        onReject: () -> Unit
    ): InterfaceLease? {
        synchronized(registry) {
            val iface = registry[interfaceName] ?: run {
                onReject()
                return null
            }

            if (!isModeAllowed(iface, requestedMode)) {
                log.info("[V2] Grensesnitt $interfaceName støtter ikke modusen $requestedMode")
                onReject()
                return null
            }

            val modeMap = activeLeases.getOrPut(interfaceName) { mutableMapOf() }
            val tracker = modeMap.getOrPut(requestedMode) { LeaseTracker(interfaceName, requestedMode, 0) }

            // Sjekk om grensesnittet er opptatt av EN ANNEN modus
            if (iface.mode != NetworkInterfaceMode.Idle && iface.mode != requestedMode && modeMap.isNotEmpty()) {
                log.info("[V2] $interfaceName er opptatt i modus ${iface.mode}, kan ikke gi tilgang til $requestedMode")
                onReject()
                return null
            }

            // Hvis modusen ikke er satt på enheten enda, sett den
            if (iface.mode != requestedMode) {
                registry[interfaceName] = withMode(iface, requestedMode)
                log.info("[V2] Grensesnitt $interfaceName satt til modus $requestedMode")
            }

            // Øk referansetellingen for denne modusen
            tracker.refCount++
            log.debug("[V2] Acquire lease for $requestedMode på $interfaceName. Referanser: ${tracker.refCount}")

            return InterfaceLease(
                interfaceName = interfaceName,
                mode = requestedMode,
                releaseAction = { name, m -> release(name, m) },
                forceReleaseAction = { name -> forceReleaseAll(name) }
            )
        }
    }

    fun <T> releaseLease(interfaceName: String, mode: NetworkInterfaceMode, block: (InterfaceLease) -> T): T {
        val existingLease = getActiveLease(interfaceName, mode)
        val lease = existingLease ?: InterfaceLease(
            interfaceName = interfaceName,
            mode = mode,
            releaseAction = { name, _ -> forceReleaseAll(name) },
            forceReleaseAction = { name -> forceReleaseAll(name) }
        )

        var success = false
        val result = try {
            val res = block(lease)
            success = true
            res
        } catch (e: Exception) {
            log.error("[V2] Feil under releaseLease for $mode på $interfaceName. Tvinger resetting.", e)
            lease.forceRelease()
            throw e
        } finally {
            if (success) {
                if (existingLease != null) {
                    lease.release()
                } else {
                    // Hvis den var fake/mangler, sørg for at vi uansett tvinger en total opprydding
                    lease.forceRelease()
                }
            }
        }
        return result
    }

    fun getActiveLease(interfaceName: String, mode: NetworkInterfaceMode): InterfaceLease? {
        synchronized(registry) {
            val tracker = activeLeases[interfaceName]?.get(mode)
            if (tracker != null && tracker.refCount > 0) {
                return InterfaceLease(
                    interfaceName = interfaceName,
                    mode = mode,
                    releaseAction = { name, m -> release(name, m) },
                    forceReleaseAction = { name -> forceReleaseAll(name) }
                )
            }
            return null
        }
    }

    /**
     * Tvinger frigjøring av alle leaser på et grensesnitt (nyttig ved ekstern deaktivering/feil fra NetworkManager)
     */
    fun forceReleaseAll(interfaceName: String) {
        synchronized(registry) {
            val modeMap = activeLeases.remove(interfaceName)
            if (modeMap != null) {
                log.warn("[V2] FORCE RELEASE: Tvinger frigjøring av alle leaser på $interfaceName pga. ekstern deaktivering")
                val iface = registry[interfaceName] ?: return
                if (iface.mode != NetworkInterfaceMode.Idle) {
                    registry[interfaceName] = withMode(iface, NetworkInterfaceMode.Idle)
                    log.info("[V2] $interfaceName tvunget til Idle")
                }
            }
        }
    }

    private fun release(interfaceName: String, mode: NetworkInterfaceMode) {
        synchronized(registry) {
            val modeMap = activeLeases[interfaceName] ?: return
            val tracker = modeMap[mode] ?: return

            tracker.refCount--
            log.debug("[V2] Release $mode på $interfaceName. Rest: ${tracker.refCount}")

            if (tracker.refCount <= 0) {
                modeMap.remove(mode)
            }

            // Sjekk om registeret skal tømmes helt
            if (modeMap.isEmpty()) {
                activeLeases.remove(interfaceName)
                val iface = registry[interfaceName] ?: return
                // Bare sett til Idle hvis vi ikke allerede er det
                if (iface.mode != NetworkInterfaceMode.Idle) {
                    registry[interfaceName] = withMode(iface, NetworkInterfaceMode.Idle)
                    log.info("[V2] $interfaceName satt til Idle")
                }
            }
        }
    }

    private fun withMode(iface: NetworkInterface, mode: NetworkInterfaceMode): NetworkInterface {
        return when (iface) {
            is WirelessNetworkInterface -> iface.copy(mode = mode)
            is EthernetNetworkInterface -> iface.copy(mode = mode)
            else -> iface
        }
    }

    private fun isModeAllowed(iface: NetworkInterface, mode: NetworkInterfaceMode): Boolean {
        return when (mode) {
            NetworkInterfaceMode.Tether -> {
                val wifi = iface as? WirelessNetworkInterface ?: return false
                wifi.caps.any { it == WirelessNetworkInterfaceCapability.AP || it == WirelessNetworkInterfaceCapability.Concurrent }
            }
            else -> true
        }
    }

    fun getInterfaces(type: NetworkInterfaceType, desiredMode: NetworkInterfaceMode): List<InterfaceAvailability> {
        return getInterfaces(type, setOf(desiredMode))
    }

    fun getInterfaces(type: NetworkInterfaceType, desiredModes: Set<NetworkInterfaceMode>): List<InterfaceAvailability> {
        return registry.values.filter { it.type == type }.map { nif ->
            val isIdle = nif.mode == NetworkInterfaceMode.Idle
            val isCompatible = nif.mode in desiredModes

            val isAvailable = when {
                desiredModes.contains(NetworkInterfaceMode.Tether) -> {
                    isModeAllowed(nif, NetworkInterfaceMode.Tether) && (isIdle || isCompatible)
                }
                else -> isIdle || isCompatible
            }

            InterfaceAvailability(nif = nif, isAvailable = isAvailable)
        }
    }
}