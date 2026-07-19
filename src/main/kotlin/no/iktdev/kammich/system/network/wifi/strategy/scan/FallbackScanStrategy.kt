package no.iktdev.kammich.system.network.wifi.strategy.scan

import no.iktdev.kammich.models.internal.network.WifiScanState
import no.iktdev.kammich.system.SysCommand
import no.iktdev.kammich.system.network.wifi.parser.WifiScanResultParser
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(999)
class FallbackScanStrategy(
    private val exec: SysCommand,
    private val scanResultParser: WifiScanResultParser
): WifiScanStrategy {
    private val log = LoggerFactory.getLogger(FallbackScanStrategy::class.java)


    override fun scan(interfaceName: String): WifiScanState {
        val result = exec.sudo("iw", "dev", interfaceName, "scan").getOrNull() ?: throw RuntimeException("Could not find the scan")
        val out = scanResultParser.usingJc(result)?.let { x -> scanResultParser.parseJson(x) } ?: throw RuntimeException("Could not find the scan")
        val fes = scanResultParser.iwToFeNetwork(interfaceName, out)
        return WifiScanState(
            networks = fes
        )
    }

    override fun isSupported(): Boolean {
        log.info("is always supported")
        return true
    }
}