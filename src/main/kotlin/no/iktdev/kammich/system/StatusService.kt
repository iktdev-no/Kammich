package no.iktdev.kammich.system

import no.iktdev.kammich.models.shared.CaptivePortalMonitorStatus
import no.iktdev.kammich.models.shared.NetworkInterfaceScannerStatus
import no.iktdev.kammich.models.shared.ServiceStatus
import no.iktdev.kammich.system.network.CaptivePortalMonitorService
import no.iktdev.kammich.system.network.NetworkInterfaceScannerV2
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class StatusService(
    private val captivePortalMonitorService: CaptivePortalMonitorService,
    private val networkInterfaceScannerV2: NetworkInterfaceScannerV2
) {

    fun getServiceStatus(): ServiceStatus {
        return ServiceStatus(
            captivePortalMonitor = CaptivePortalMonitorStatus(
                alive = captivePortalMonitorService.isAlive(),
                lastCheck = captivePortalMonitorService.getLastCheck(),
            ),
            networkInterfaceScannerStatus = NetworkInterfaceScannerStatus(
                alive = networkInterfaceScannerV2.isAlive(),
                lastScan = networkInterfaceScannerV2.getLastScan()
            )
        )
    }
}
