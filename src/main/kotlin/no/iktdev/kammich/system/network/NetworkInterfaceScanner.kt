package no.iktdev.kammich.system.network

import no.iktdev.kammich.models.shared.network.EthernetNetworkInterface
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode
import no.iktdev.kammich.models.shared.network.NetworkInterfaceType
import no.iktdev.kammich.models.shared.network.WirelessNetworkInterface
import no.iktdev.kammich.system.SysCommand
import no.iktdev.kammich.system.network.wifi.WifiTetherService
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class NetworkInterfaceScanner(
    private val networkInterfaceRegistry: NetworkInterfaceRegistry,
    private val exec: SysCommand
) {
    private val ignoredTypes = setOf("loopback", "tun", "wifi-p2p")

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReadyEvent(event: ApplicationReadyEvent) {
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

                val mode = when {
                    isExternal -> NetworkInterfaceMode.External
                    interfaceState != "connected" -> NetworkInterfaceMode.Idle
                    else -> if (connName == WifiTetherService.ap_profileName) NetworkInterfaceMode.Master else NetworkInterfaceMode.Client
                }

                val nif = when (interfaceType) {
                    NetworkInterfaceType.Wifi -> WirelessNetworkInterface(
                        interfaceName = interfaceName,
                        macAdress = mac, // Her legger du inn MACen du hentet
                        mode = mode
                    )
                    NetworkInterfaceType.Ethernet -> EthernetNetworkInterface(
                        interfaceName = interfaceName,
                        macAdress = mac,
                        mode = mode
                    )
                    else -> null
                }
                if (nif != null) {
                    networkInterfaceRegistry.registerOrUpdate(nif)
                }
            }
    }

    private fun getDeviceInfo(interfaceName: String): Pair<String, String?> {
        // -g står for "get", GENERAL.HWADDR henter macen, GENERAL.CONNECTION henter profilen
        val cmd = exec.nonSudo("nmcli", "-g", "GENERAL.HWADDR,GENERAL.CONNECTION", "device", "show", interfaceName)
        val output = cmd.getOrNull()?.split(":") ?: return "unknown" to null

        // Output blir ofte "XX:XX:XX:XX:XX:XX:NameOfConnection"
        // Vi må være litt forsiktige hvis navnet inneholder kolon, men for MAC er det trygt
        val mac = output.take(6).joinToString(":")
        val connName = output.drop(6).joinToString(":").trim()

        return mac to (connName.takeIf { it.isNotEmpty() })
    }

    // Inne i NetworkInterfaceScanner
    private fun getActiveMode(interfaceName: String): NetworkInterfaceMode {
        // Spør NM om den aktive forbindelsen på dette interfacet
        val cmd = exec.nonSudo("nmcli", "-g", "GENERAL.CONNECTION", "device", "show", interfaceName)
        val connectionName = cmd.getOrNull()?.trim() ?: return NetworkInterfaceMode.Idle

        // Sjekk om dette er vår egen hotspot-profil
        if (connectionName == "kammich-ap") return NetworkInterfaceMode.Master

        // Hvis vi er tilkoblet noe annet, er vi Client/Backhaul
        return NetworkInterfaceMode.Client
    }

}