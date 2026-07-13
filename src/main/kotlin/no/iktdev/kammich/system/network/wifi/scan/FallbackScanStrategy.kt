package no.iktdev.kammich.system.network.wifi.scan

import no.iktdev.kammich.models.internal.network.IwScanItem
import no.iktdev.kammich.models.internal.network.WifiScanResult
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiScanState
import no.iktdev.kammich.system.network.wifi.WifiRunner
import no.iktdev.kammich.system.network.wifi.parser.WifiScanResultParser
import no.iktdev.kammich.system.network.wifi.pipeJc
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import kotlin.collections.set

@Component
@Order(999)
class FallbackScanStrategy(
    private val wifiRunner: WifiRunner,
    private val scanResultParser: WifiScanResultParser
): WifiScanStrategy {
    private val log = LoggerFactory.getLogger(FallbackScanStrategy::class.java)


    override fun scan(interfaceName: String): WifiScanResult {
        val result = wifiRunner.run("sudo", "iw", "dev", interfaceName, "scan")
            .pipeJc()
        val out = when (result) {
            is WifiRunner.CommandResult.Success -> {
                scanResultParser.parseJson(result.output)
            }
            is WifiRunner.CommandResult.Failure -> {
                log.error("Scan error: ${result.error}, exit: ${result.exitCode}")
                return WifiScanResult(
                    networks = emptyList(),
                    state = WifiScanState.ERROR,
                    success = false,
                )
            }
        }
        val fes = scanResultParser.iwToFeNetwork(interfaceName, out)
        return WifiScanResult(
            networks = fes,
            state = WifiScanState.IDLE,
            success = true,
        )
    }

    override fun isSupported(): Boolean {
        log.info("is always supported")
        return true
    }
}