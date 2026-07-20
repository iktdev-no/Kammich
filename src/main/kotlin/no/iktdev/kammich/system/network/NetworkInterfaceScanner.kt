package no.iktdev.kammich.system.network

import no.iktdev.kammich.models.internal.network.InterfaceResolution
import no.iktdev.kammich.models.shared.network.EthernetNetworkInterface
import no.iktdev.kammich.models.shared.network.InterfaceActiveState
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode
import no.iktdev.kammich.models.shared.network.NetworkInterfaceType
import no.iktdev.kammich.models.shared.network.WirelessNetworkInterface
import no.iktdev.kammich.models.shared.network.WirelessNetworkInterfaceCapability
import no.iktdev.kammich.models.shared.network.WirelessTetheringState
import no.iktdev.kammich.system.SysCommand
import no.iktdev.kammich.system.network.wifi.WifiConnectivityService
import no.iktdev.kammich.system.network.wifi.WifiTetherService
import no.iktdev.kammich.system.network.wifi.parser.WifiPhyInfoParser
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class NetworkInterfaceScanner(
    private val networkInterfaceRegistry: NetworkInterfaceRegistry,
    private val repository: NetworkStateRepository,
    private val exec: SysCommand,
    private val wifiTetherService: WifiTetherService,
    private val wifiConnectivityService: WifiConnectivityService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val ignoredTypes = setOf("loopback", "tun", "wifi-p2p")

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReadyEvent(event: ApplicationReadyEvent) {
        loadStates()
    }

    @Scheduled(fixedRate = 5000)
    fun loadStates() {
        val out = exec.nonSudo("nmcli", "-t", "-f", "DEVICE,TYPE,STATE", "device").getOrNull() ?: return
        out.lines()
            .filter { it.isNotBlank() }
            .map { it.split(":") }
            .filter { it.size >= 3 }
            .filter { it[1] !in ignoredTypes }
            .map { it ->
                val interfaceName = it[0]
                val interfaceType = it[1].let { x ->
                    when (x) {
                        NetworkInterfaceType.Wifi.name.lowercase() -> NetworkInterfaceType.Wifi
                        NetworkInterfaceType.Ethernet.name.lowercase() -> NetworkInterfaceType.Ethernet
                        else -> null
                    }
                }
                val interfaceState = it[2].split(" ")[0]
                val isExternal = it[2].contains("externally")

                val (mac, connName) = getDeviceInfo(interfaceName)

                val mode = determineMode(interfaceState, connName, isExternal)
                val currentState = when (interfaceType) {
                    NetworkInterfaceType.Wifi -> getActiveMode(interfaceName)
                    else -> null
                }
                repository

                val nif = when (interfaceType) {
                    NetworkInterfaceType.Wifi -> {
                        val caps = getWirelessCapabilities(interfaceName)
                        WirelessNetworkInterface(
                            interfaceName = interfaceName,
                            macAdress = mac, // Her legger du inn MACen du hentet
                            mode = mode,
                            caps = caps,
                        )
                    }
                    NetworkInterfaceType.Ethernet -> EthernetNetworkInterface(
                        interfaceName = interfaceName,
                        macAdress = mac,
                        mode = mode
                    )
                    else -> null
                }
                if (nif != null) {
                    networkInterfaceRegistry.registerOrUpdate(nif)
                    if (currentState?.stateObject != null) {
                        if (nif is WirelessNetworkInterface) {
                            repository.updateInterface( currentState, nif)
                        }
                    }
                }
            }
    }

    private fun getDeviceInfo(interfaceName: String): Pair<String, String?> {
        val cmd = exec.nonSudo("nmcli", "-t", "-f", "GENERAL.HWADDR,GENERAL.CONNECTION", "device", "show", interfaceName)
        val output = cmd.getOrNull() ?: return "unknown" to null

        // Splitter på linjeskift for å få de to linjene
        val lines = output.lines()

        // Finn MAC-adressen ved å se etter linjen som starter med GENERAL.HWADDR:
        val mac = lines.find { it.startsWith("GENERAL.HWADDR:") }
            ?.substringAfter("GENERAL.HWADDR:") ?: "unknown"

        // Finn connection-navnet ved å se etter linjen som starter med GENERAL.CONNECTION:
        // Merk: Vi bruker substringAfter første ':' for å håndtere hvis navnet selv inneholder ':'
        val connName = lines.find { it.startsWith("GENERAL.CONNECTION:") }
            ?.substringAfter("GENERAL.CONNECTION:")
            ?.takeIf { it.isNotBlank() }

        return mac to connName
    }

    // Gjør denne funksjonen til din "Source of Truth" for modus-logikk
    private fun determineMode(state: String, connName: String?, isExternal: Boolean): NetworkInterfaceMode {
        if (isExternal) return NetworkInterfaceMode.External
        if (state != "connected") return NetworkInterfaceMode.Idle

        return when (connName) {
            WifiTetherService.ap_profileName -> NetworkInterfaceMode.Master
            else -> NetworkInterfaceMode.Client
        }
    }

    private fun getActiveMode(interfaceName: String): InterfaceResolution {
        val cmd = exec.nonSudo("nmcli", "-t", "-f", "GENERAL.CONNECTION", "device", "show", interfaceName)
        val output = cmd.getOrNull()?.trim() ?: return InterfaceResolution(NetworkInterfaceMode.Idle, InterfaceActiveState.Disconnected)

        if (output.isBlank() || output == ":")
            return InterfaceResolution(NetworkInterfaceMode.Idle, InterfaceActiveState.Disconnected)

        // Her kaller du strategiene dine
        return when (output) {
            WifiTetherService.ap_profileName -> {
                val state = wifiTetherService.getStrategy()?.getState(interfaceName)
                InterfaceResolution(NetworkInterfaceMode.Master, if (state?.state == WirelessTetheringState.Broadcasting) InterfaceActiveState.Tethering else InterfaceActiveState.Idle, state)
            }
            else -> {
                val state = wifiConnectivityService.getActiveStrategy()?.getState(interfaceName)
                InterfaceResolution(NetworkInterfaceMode.Client,
                state?.state ?: InterfaceActiveState.Disconnected,
                    state)
            }
        }
    }

    private fun getPhyFromInterface(interfaceName: String): String? {
        val output = exec.nonSudo("iw", "dev", interfaceName, "info").getOrNull() ?: return null
        return output.lines()
            .map { it.trim() }
            .find { it.startsWith("wiphy") }
            ?.split(Regex("\\s+")) // Splitter på alle typer whitespace (space, tab, etc.)
            ?.lastOrNull()         // Henter ut det siste elementet (tallet)
            ?.let { "phy$it" }     // Konverterer til "phy0"
    }

    private fun getWirelessCapabilities(interfaceName: String): Set<WirelessNetworkInterfaceCapability> {
        val phy = getPhyFromInterface(interfaceName) ?: run {
            log.info("Could not find pyh network interface {}", interfaceName)
            return emptySet()
        }
        val phyOut = exec.nonSudo("iw", "phy", phy, "info").getOrNull() ?: run { log.info("Could not find phy network interface {}", phy); return emptySet() }
        return WifiPhyInfoParser().parseCapabilities(phyOut)
    }

}