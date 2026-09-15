package no.iktdev.kammich.models.shared

import java.time.Instant

data class ServiceStatus(
    val captivePortalMonitor: CaptivePortalMonitorStatus,
    val networkInterfaceScannerStatus: NetworkInterfaceScannerStatus
)

data class CaptivePortalMonitorStatus(
    val alive: Boolean,
    val lastCheck: Instant?,
)

data class NetworkInterfaceScannerStatus(
    val alive: Boolean,
    val lastScan: Instant?,
)