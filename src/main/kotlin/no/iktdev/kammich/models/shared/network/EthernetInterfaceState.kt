package no.iktdev.kammich.models.shared.network

import no.iktdev.kammich.models.internal.network.NmCliDeviceState
import java.time.Instant

data class EthernetInterfaceState(
    val ifName: String,
    val state: EthernetInterfaceStateType,
    val carrier: Boolean,
    val ipv4: String? = null,
    val deviceState: NmCliDeviceState? = null,
    val emergencyAllowed: Boolean = false,
    val emergencyAt: Instant? = null,
    val error: EthernetInterfaceClientError? = null,
    val operatingMode: NetworkInterfaceMode = NetworkInterfaceMode.Idle
)

enum class EthernetInterfaceClientError {
    Unknown
}

enum class EthernetInterfaceStateType {
    Idle,
    Disconnected,
    Connected,
    Connecting,
    Emergency
}

data class EmergencyModeRequest(
    val enabled: Boolean
)