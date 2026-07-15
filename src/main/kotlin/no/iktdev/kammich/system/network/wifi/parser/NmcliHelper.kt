package no.iktdev.kammich.system.network.wifi.parser

abstract class NmcliHelper {
    open fun isSupported(): Boolean {
        return try {
            Runtime.getRuntime().exec("which nmcli").waitFor() == 0
        } catch (e: Exception) { false }
    }
}