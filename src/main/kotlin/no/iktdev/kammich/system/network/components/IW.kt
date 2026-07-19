package no.iktdev.kammich.system.network.components

import no.iktdev.kammich.system.SysCommand
import org.springframework.stereotype.Component

@Component
class IW(
    private val exec: SysCommand
) {
    fun getMode(ifname: String): InterfaceMode {
        val result = exec.nonSudo("iw", "dev", ifname, "info")
        val out = result.getOrNull() ?:
            return InterfaceMode.Unavailable
        val type = out.lines()
            .filter { it -> it.trim().startsWith("type") }
            .map { it.trim() }
            .map { it.substringAfter("type") }
            .singleOrNull() ?: return InterfaceMode.Unavailable

        return when (type) {
            "AP" -> InterfaceMode.AP
            "P2P-device" -> InterfaceMode.P2PDevice
            else -> InterfaceMode.Unavailable
        }
    }

    enum class InterfaceMode {
        Unavailable,
        AP,
        P2PDevice
    }
}