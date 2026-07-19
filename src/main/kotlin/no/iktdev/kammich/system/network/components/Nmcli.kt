package no.iktdev.kammich.system.network.components

import no.iktdev.kammich.storage.provider.GPhoto2StorageProvider
import no.iktdev.kammich.system.SysCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Nmcli(
    private val exec: SysCommand
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun general(iface: String): NmcliGeneral? {
        val result = exec.nonSudo("nmcli", "-t", "-f",
            General.entries.joinToString(",") { it.type }, "device", "show", iface).getOrNull() ?: return null
        val data = result.lines().filter { it ->
            if (!it.contains(":")) {
                log.warn("Line does not contain ':' -> $it")
            }
            it.contains(":")
        }.associate { line ->
            val (k, v) = line.split(":", limit = 2)
            val key = try {
                General.toKey(k)
            } catch (e: IllegalArgumentException) {
                log.error("Failed to make $k into General..")
                return null
            }
             key to v
        }

        fun get(g: General) = data[g] ?: ""
        fun getOrNull(g: General) = data[g]
        fun getBool(g: General) = get(g).lowercase() == "yes"
        fun getInt(g: General) = get(g).substringBefore(" ").toIntOrNull() ?: 0

        return NmcliGeneral(
            device = get(General.DEVICE),
            type = get(General.TYPE),
            nmType = get(General.NM_TYPE),
            dbusPath = get(General.DBUS_PATH),
            vendor = get(General.VENDOR),
            product = get(General.PRODUCT),
            driverVersion = get(General.DRIVER_VERSION),
            formwareVersion = get(General.FIRMWARE_VERSION),
            hwAddr = get(General.HWADDRESS),
            mtu = getInt(General.MTU_SIZE),
            state = NmState.fromCode(getInt(General.STATE)),
            reason = getInt(General.REASON),
            ipv4Connectivity = getInt(General.IP4),
            ipv6Connectivity = getInt(General.IP6),
            udi = get(General.UDI),
            path = get(General.PATH),
            ipIface = get(General.IP_IFACE),
            isSoftware = getBool(General.IS_SOFTWARE),
            nmManaged = getBool(General.NM_MANAGED),
            autoConnect = getBool(General.AUTOCONNECT),
            firmwareMissing = getBool(General.FIRMWARE_MISSING),
            nmPluginMissing = getBool(General.NM_PLUGIN_MISSING),
            physPortId = get(General.PHYS_PORT_ID).takeIf { it.isNotEmpty() },
            connection = get(General.CONNECTION).takeIf { it != "--" },
            conUuid = get(General.CON_UUID),
            conPath = get(General.CON_PATH),
            metered = getBool(General.METERED)
        )
    }

    fun wifiProperties(iface: String): NmcliWifiProperties? {
        val result = exec.nonSudo("nmcli", "-t", "-f", WifiProperties.entries.joinToString(""), "device", "show", iface).getOrNull() ?: return null
        val data = result.lines().associate { line ->
            val (k, v) = line.split(":", limit = 2)
            WifiProperties.valueOf(k) to v
        }
        fun getBool(w: WifiProperties) = data[w] == "yes"

        return NmcliWifiProperties(
            wep = getBool(WifiProperties.WEP),
            wpa = getBool(WifiProperties.WPA),
            wpa2 = getBool(WifiProperties.WPA2),
            tkip = getBool(WifiProperties.TKIP),
            ccmp = getBool(WifiProperties.CCMP),
            ap = getBool(WifiProperties.AP),
            adhoc = getBool(WifiProperties.ADHOC),
            ghz2 = getBool(WifiProperties.GHZ2),
            ghz5 = getBool(WifiProperties.GHZ5),
            ghz6 = getBool(WifiProperties.GHZ6),
            mesh = getBool(WifiProperties.MESH),
            ibssRsn = getBool(WifiProperties.IBSS_RSN)
        )
    }

    fun wifi(): List<NmcliWifi> {
        val fields = listOf("IN-USE", "SSID", "DEVICE", "ACTIVE", "SECURITY", "SIGNAL", "FREQ", "CHAN", "BANDWIDTH", "BSSID")
        val result = exec.nonSudo("nmcli", "-t", "-f", fields.joinToString(","), "dev", "wifi").getOrNull() ?: ""
        return result.lines().filter { it.contains(":") }.mapNotNull { line ->
            try {
                val columns = line.split(":", limit = fields.size)
                NmcliWifi(
                    inUse = columns[0] == "*",
                    ssid = columns[1].takeIf { it.isNotEmpty() && it != "--" },
                    device = columns[2],
                    active = columns[3] == "yes",
                    security = columns[4],
                    signal = columns[5].toIntOrNull() ?: -1,
                    frequency = columns[6],
                    channel = columns[7].toInt(),
                    bandwidth = columns[8],
                    bssid = columns[9].replace("\\s".toRegex(), ""),
                )
            } catch (e: Exception) {
                log.error("Kunne ikke parse :$line:")
                e.printStackTrace()
                null
            }
        }
    }

    data class NmcliIPv4(
        val address: String,
        val gateway: String,
        val routes: List<String>,
        val dns: List<String>,
        val domain: List<String>,
    )

    data class NmcliWifi(
        val inUse: Boolean,
        val ssid: String? = null,
        val device: String,
        val active: Boolean,
        val security: String,
        val signal: Int,
        val frequency: String,
        val channel: Int,
        val bandwidth: String,
        val bssid: String? = null,
    )

    data class NmcliGeneral(
        val device: String,
        val type: String,
        val nmType: String,
        val dbusPath: String,
        val vendor: String,
        val product: String,
        val driverVersion: String,
        val formwareVersion: String,
        val hwAddr: String,
        val mtu: Int,
        val state: NmState,
        val reason: Int,
        val ipv4Connectivity: Int,
        val ipv6Connectivity: Int,
        val udi: String,
        val path: String,
        val ipIface: String,
        val isSoftware: Boolean,
        val nmManaged: Boolean,
        val autoConnect: Boolean,
        val firmwareMissing: Boolean,
        val nmPluginMissing: Boolean,
        val physPortId: String? = null,
        val connection: String? = null,
        val conUuid: String,
        val conPath: String,
        val metered: Boolean,
    )

    enum class NmState(val code: Int) {
        UNKNOWN(0),
        DISCONNECTED(20),
        PREPARE(40),
        CONFIG(50),
        NEED_AUTH(60),
        IP_CONFIG(70),
        IP_CHECK(80),
        SECONDARIES(90),
        CONNECTED(100);

        companion object {
            fun fromCode(code: Int) = entries.find { it.code == code } ?: UNKNOWN
        }
    }

    data class NmcliWifiProperties(
        val wep: Boolean,
        val wpa: Boolean,
        val wpa2: Boolean,
        val tkip: Boolean,
        val ccmp: Boolean,
        val ap: Boolean,
        val adhoc: Boolean,
        val ghz2: Boolean,
        val ghz5: Boolean,
        val ghz6: Boolean,
        val mesh: Boolean,
        val ibssRsn: Boolean,
    )

    enum class General(val type: String) {
        DEVICE("GENERAL.DEVICE"),
        TYPE("GENERAL.TYPE"),
        NM_TYPE("GENERAL.NM-TYPE"),
        DBUS_PATH("GENERAL.DBUS-PATH"),
        VENDOR("GENERAL.VENDOR"),
        PRODUCT("GENERAL.PRODUCT"),
        DRIVER_VERSION("GENERAL.DRIVER-VERSION"),
        FIRMWARE_VERSION("GENERAL.FIRMWARE-VERSION"),
        HWADDRESS("GENERAL.HWADDR"),
        MTU_SIZE("GENERAL.MTU"),
        STATE("GENERAL.STATE"),
        REASON("GENERAL.REASON"),
        IP4("GENERAL.IP4-CONNECTIVITY"),
        IP6("GENERAL.IP6-CONNECTIVITY"),
        UDI("GENERAL.UDI"),
        PATH("GENERAL.PATH"),
        IP_IFACE("GENERAL.IP-IFACE"),
        IS_SOFTWARE("GENERAL.IS-SOFTWARE"),
        NM_MANAGED("GENERAL.NM-MANAGED"),
        AUTOCONNECT("GENERAL.AUTOCONNECT"),
        FIRMWARE_MISSING("GENERAL.FIRMWARE-MISSING"),
        NM_PLUGIN_MISSING("GENERAL.NM-PLUGIN-MISSING"),
        PHYS_PORT_ID("GENERAL.PHYS-PORT-ID"),
        CONNECTION("GENERAL.CONNECTION"),
        CON_UUID("GENERAL.CON-UUID"),
        CON_PATH("GENERAL.CON-PATH"),
        METERED("GENERAL.METERED"),
        ;
        companion object {
            fun toKey(type: String): General {
                return General.entries.find { it.type == type } ?: throw IllegalArgumentException("Unknown general type: $type")
            }
        }
    }

    enum class WifiProperties(val type: String) {
        WEP("WIFI-PROPERTIES.WEP"),
        WPA("WIFI-PROPERTIES.WPA"),
        WPA2("WIFI-PROPERTIES.WPA2"),
        TKIP("WIFI-PROPERTIES.TKIP"),
        CCMP("WIFI-PROPERTIES.CMPMP"),
        AP("WIFI-PROPERTIES.AP"),
        ADHOC("WIFI-PROPERTIES.ADHOC"),
        GHZ2("WIFI-PROPERTIES.2GHZ"),
        GHZ5("WIFI-PROPERTIES.5GHZ"),
        GHZ6("WIFI-PROPERTIES.6GHZ"),
        MESH("WIFI-PROPERTIES.MESH"),
        IBSS_RSN("WIFI-PROPERTIES.IBSS-RSN"),
    }
}