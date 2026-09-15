package no.iktdev.kammich.system.network.al

import no.iktdev.kammich.models.internal.network.NmCliDevice
import no.iktdev.kammich.models.internal.network.NmCliDeviceState
import no.iktdev.kammich.models.internal.network.NmCliDeviceType
import no.iktdev.kammich.models.shared.network.InterfaceMode
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiNetworkHardwareMode
import no.iktdev.kammich.system.SysCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NmcliAL(private val exec: SysCommand) : INmcliAL {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun toInterfaceType(type: String): NmCliDeviceType {
        return NmCliDeviceType.findBy(type)
    }

    private fun toState(state: String): NmCliDeviceState {
        val useState = state.split(" ").first()
        return NmCliDeviceState.findByName(useState)
    }

    private fun isExternal(state: String): Boolean {
        val useState = state.split(" ").lastOrNull()
        return useState?.contains("external") ?: false
    }


    override fun isDeviceManaged(ifName: String): Boolean {
        val out = exec.nonSudo(
            "nmcli", "-t", "-f", "GENERAL.NM-MANAGED",
            "device", "show", ifName
        ).getOrNull() ?: return false

        return out
            .substringAfter("GENERAL.NM-MANAGED:", "")
            .trim()
            .equals("yes", ignoreCase = true)
    }

    override fun getDevices(): List<NmCliDevice> {
        val out = exec.nonSudo("nmcli", "-t", "-f", "DEVICE,TYPE,STATE", "device").getOrNull() ?: run {
            log.error("Could not get devices")
            return emptyList()
        }
        return out.lines().asSequence().filter { it.isNotBlank() }.map { it.split(":") }
            .filter { it.size >= 3 }
            .map { (name, type, state) ->
                NmCliDevice(
                    name,
                    toInterfaceType(type),
                    toState(state),
                    isExternal(state),
                )
            }
            .filter { !it.ifName.startsWith("veth") }.toList()
    }

    override fun getDeviceHWADDR(ifName: String): String? {
        return exec.nonSudo("nmcli", "-t", "-f", "GENERAL.HWADDR", "device", "show", ifName)
            .getOrNull()
            ?.substringAfter("GENERAL.HWADDR:")?.trim()
    }

    /**
     * @return Returns the name of the active connection, no name, no connection
     */
    override fun getConnectionName(ifName: String): String? {
        return exec.nonSudo("nmcli", "-t", "-f", "GENERAL.CONNECTION", "device", "show", ifName)
            .getOrNull()
            ?.trim()
            ?.substringAfter("GENERAL.CONNECTION:")
            ?.trim()
            ?.takeIf { it.isNotBlank() && it != "--" }
    }

    override fun getWirelessMode(connectionName: String): InterfaceMode? {
        val out = exec.nonSudo("nmcli", "-t", "-f", "802-11-wireless.mode", "connection", "show", connectionName)
            .getOrNull() ?: run {
            log.error("Could not get Connection mode")
            return null
        }
        val mode = out.substringAfter("802-11-wireless.mode:").trim().lowercase()
        return when (mode) {
            "ap" -> InterfaceMode.Tether
            "client", "infrastructure" -> InterfaceMode.Client
            "mesh" -> InterfaceMode.Mesh
            "adhoc" -> InterfaceMode.AdHoc
            else -> InterfaceMode.Idle
        }
    }

    override fun getEthernetMode(connectionName: String): InterfaceMode? {
        val out = exec.nonSudo(
            "nmcli",
            "-t",
            "-f", "ipv4.method",
            "connection",
            "show",
            connectionName
        ).getOrNull() ?: run {
            log.error("Could not get Ethernet connection mode")
            return null
        }

        val method = out
            .substringAfter("ipv4.method:")
            .trim()
            .lowercase()

        return when (method) {
            "auto" -> InterfaceMode.Client
            "shared" -> InterfaceMode.Tether
            "manual", "disabled", "link-local" -> InterfaceMode.Idle
            else -> {
                log.warn(
                    "Unknown Ethernet IPv4 method '{}' for connection '{}'",
                    method,
                    connectionName
                )
                InterfaceMode.Idle
            }
        }
    }


    override fun deleteConnection(connectionName: String): Boolean {
        return exec.sudo("nmcli", "con", "delete", connectionName).isSuccess()
    }

    override fun setAutoConnect(connectionName: String, autoConnect: Boolean): Boolean {
        val useValue = if (autoConnect) "yes" else "no"
        return exec.sudo("nmcli", "con", "modify", connectionName, "connection.autoconnect", useValue).isSuccess()
    }

    override fun getConnectionIpv4Address(
        connectionName: String
    ): String? {
        val out = exec.nonSudo(
            "nmcli",
            "-t",
            "-f", "ipv4.addresses",
            "connection",
            "show",
            connectionName
        ).getOrNull() ?: return null

        return out
            .substringAfter("ipv4.addresses:", "")
            .trim()
            .takeIf { it.isNotBlank() }
    }

    override fun getDeviceIpv4Address(interfaceName: String): String? {
        val result = exec.sudo("nmcli", "-t", "-f", "IP4.ADDRESS", "device", "show", interfaceName)
        if (!result.isSuccess()) return null

        return result.getOrNull()
            ?.lineSequence()
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("IP4.ADDRESS") }
            ?.substringAfter(":")
            ?.substringBefore("/")
            ?.takeIf { it.isNotBlank() }
    }

    override fun createEthernetHostConnection(
        ifName: String,
        connectionName: String,
        ipAddress: String,
        prefix: Int,
        autoConnect: Boolean
    ): SysCommand.Result {
        val autoConnectValue = if (autoConnect) "yes" else "no"

        val command = mutableListOf(
            "nmcli", "con", "add",
            "type", "ethernet",
            "ifname", ifName,
            "con-name", connectionName,
            "ipv4.method", "shared",
            "ipv4.addresses", "$ipAddress/$prefix",
            "connection.autoconnect", autoConnectValue
        )

        return exec.sudo(*command.toTypedArray())
    }

    override fun createEthernetClientConnection(
        ifName: String,
        connectionName: String,
        autoConnect: Boolean
    ): SysCommand.Result {
        val autoConnectValue = if (autoConnect) "yes" else "no"

        val command = listOf(
            "nmcli", "con", "add",
            "type", "ethernet",
            "ifname", ifName,
            "con-name", connectionName,
            "ipv4.method", "auto",
            "connection.autoconnect", autoConnectValue
        )

        return exec.sudo(*command.toTypedArray())
    }

    override fun createWifiClientConnection(
        ifName: String,
        connectionName: String,
        ssid: String,
        bssid: String,
        password: String?,
        securityType: String?,
        autoConnect: Boolean
    ): SysCommand.Result {
        val autoConnectValue = if (autoConnect) "yes" else "no"
        val command = mutableListOf(
            "nmcli", "con", "add",
            "type", "wifi",
            "ifname", ifName,
            "con-name", connectionName,
            "ssid", ssid,
            "802-11-wireless.bssid", bssid,
            "connection.autoconnect", autoConnectValue
        )

        if (!password.isNullOrBlank()) {
            val keyMgmt = when {
                securityType?.contains("WPA3", ignoreCase = true) == true -> "sae"
                else -> "wpa-psk"
            }
            command.addAll(listOf(
                "wifi-sec.key-mgmt", keyMgmt,
                "wifi-sec.psk", password
            ))
        }

        return exec.sudo(*command.toTypedArray())
    }

    override fun createWifiTetherConnection(
        ifName: String,
        connectionName: String,
        ssid: String,
        password: String?,
        securityType: String?,
        autoConnect: Boolean
    ): SysCommand.Result {
        val autoConnectValue = if (autoConnect) "yes" else "no"

        val command = mutableListOf(
            "nmcli", "con", "add",
            "type", "wifi",
            "ifname", ifName,
            "con-name", connectionName,
            "ssid", ssid,
            "802-11-wireless.mode", "ap",
            "802-11-wireless.band", "bg",
            "ipv4.method", "shared",
            "connection.autoconnect", autoConnectValue
        )

        if (!password.isNullOrBlank()) {
            val keyMgmt = when {
                securityType?.contains("WPA3", ignoreCase = true) == true -> "sae"
                else -> "wpa-psk" // Faller tilbake på wpa-psk (WPA2) hvis type mangler eller er uspesifisert
            }
            command.addAll(listOf(
                "wifi-sec.key-mgmt", keyMgmt,
                "wifi-sec.psk", password
            ))
        }

        return exec.sudo(*command.toTypedArray())
    }

    override fun connect(connectionName: String): SysCommand.Result {
        return exec.sudo("nmcli", "--wait", "0" , "con", "up", connectionName)
    }

    override fun dropConnection(connectionName: String): Boolean {
        return exec.sudo("nmcli", "con", "down", connectionName).isSuccess()
    }

    override fun disconnect(interfaceName: String): SysCommand.Result {
        return exec.sudo("nmcli", "device", "disconnect", interfaceName)
    }

    override fun scan(ifName: String): SysCommand.Result {
        return exec.sudo("nmcli", "device", "wifi", "rescan")
    }

    override fun getNetworks(interfaceName: String): List<WifiNetwork> {
        val result = exec.nonSudo(
            "nmcli", "-t",
            "-f", "IN-USE,ACTIVE,DEVICE,SSID,SECURITY,SIGNAL,FREQ,CHAN,BANDWIDTH,BSSID",
            "device", "wifi", "list", "ifname", interfaceName
        )

        val output = result.fold(
            onSuccess = { it },
            onFailure = { out, err, code ->
                throw IllegalStateException("Klarte ikke å hente wifi-nettverk for $interfaceName (Exit code: $code). Error: $err | Output: $out")
            }
        ) ?: throw IllegalStateException("Ukjent feil ved henting av wifi-nettverk for $interfaceName")

        return output.lines().mapNotNull { parseWifiLine(it) }
    }


    private fun parseWifiLine(line: String): WifiNetwork? {
        if (!line.contains(":")) return null
        return try {
            val parts = line.split(":", limit = 10)
            if (parts.size < 10) return null

            val inUseStr = parts[0]
            val activeStr = parts[1]
            val device = parts[2]
            val ssidRaw = parts[3]
            val security = parts[4]
            val signalStr = parts[5]
            val frequencyRaw = parts[6]
            val channelStr = parts[7]
            val bandwidth = parts[8]
            val bssidRaw = parts[9]

            val ssid = ssidRaw.takeIf { it.isNotEmpty() && it != "--" } ?: ""
            val freq = frequencyRaw.split(" ")[0].toIntOrNull() ?: -1
            val bndwdth = bandwidth.split(" ")[0].toIntOrNull() ?: -1

            WifiNetwork(
                inUse = inUseStr == "*",
                isActive = activeStr == "yes",
                ssid = ssid,
                signalPercent = signalStr.toIntOrNull() ?: -1,
                isSecure = security.isNotBlank(),
                bssid = bssidRaw.replace("\\:", ":"),
                securityType = security,
                interfaceName = device,
                isHidden = ssid.isBlank(),
                channel = channelStr.toIntOrNull() ?: 0,
                frequencyMhz = freq,
                bandwidthMhz = bndwdth,
                hwMode = if (freq < 5000) WifiNetworkHardwareMode.g else WifiNetworkHardwareMode.a
            )
        } catch (e: Exception) {
            log.error("Kunne ikke parse wifi-linje: :$line:", e)
            null
        }
    }

    override fun hasEthernetCarrier(ifName: String): Boolean {
        val out = exec.nonSudo(
            "nmcli",
            "-t",
            "-f", "WIRED-PROPERTIES.CARRIER",
            "device",
            "show",
            ifName
        ).getOrNull() ?: run {
            log.error("Unable to get wired carrier prop")
            return false
        }

        return when (
            out.substringAfter("WIRED-PROPERTIES.CARRIER:", "")
                .trim()
                .lowercase()
        ) {
            "on", "yes", "true", "1" -> true
            "off", "no", "false", "0" -> false
            else -> run {
                log.error("Unable to identify wired carrier prop state")
                false
            }
        }
    }

    override fun getDeviceState(ifName: String): NmCliDeviceState {
        val out = exec.nonSudo(
            "nmcli",
            "-t",
            "-f", "GENERAL.STATE",
            "device",
            "show",
            ifName
        ).getOrNull() ?: return NmCliDeviceState.Unknown

        val code = out
            .substringAfter("GENERAL.STATE:", "")
            .substringBefore(" ")
            .trim()
            .toIntOrNull()
            ?: return NmCliDeviceState.Unknown

        return NmCliDeviceState.findByCode(code)
    }

}