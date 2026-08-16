package no.iktdev.kammich.system.network.strategy.scan

import no.iktdev.kammich.models.internal.network.WifiScanState
import no.iktdev.kammich.system.SysCommand
import no.iktdev.kammich.system.network.al.INmcliAL
import no.iktdev.kammich.system.network.v1.wifi.strategy.scan.WifiScanStrategy
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Order(2)
@Component
class NmcliScanStrategy(
    private val nmcliAL: INmcliAL,
    private val exec: SysCommand
) : WifiScanStrategy {
    private val log = LoggerFactory.getLogger(NmcliScanStrategy::class.java)

    override fun scan(interfaceName: String): WifiScanState {
        val scanStarted = nmcliAL.scan(interfaceName)
        if (scanStarted.isFailure()) {
            log.error("Could not scan for $interfaceName")
            return WifiScanState(
                networks = emptyList(),
                error = "Could not scan for $interfaceName",
            )
        }

        return try {
            val nets = nmcliAL.getNetworks(interfaceName)
            WifiScanState(nets)
        } catch (e: Exception) {
            log.error("Could not get networks for $interfaceName")
            return WifiScanState(emptyList(), error = "Could not get networks for $interfaceName\n${e.message}")
        }
    }

    override fun isSupported(): Boolean {
        // Vi sjekker om 'nmcli' eksisterer ved å prøve å kjøre kommandoen raskt
        // Dette er mer effektivt enn Runtime.exec hver gang
        val result = exec.nonSudo("which", "nmcli")
        return result.isSuccess()
    }
}