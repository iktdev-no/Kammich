package no.iktdev.kammich.models.shared.network

import no.iktdev.kammich.models.internal.network.NmCliDeviceState
import java.time.Instant

data class EthernetConnection(
    val ifName: String,
    val state: EthernetConnectionStateType,
    val carrier: Boolean,
    val ipv4: String? = null,
    val deviceState: NmCliDeviceState? = null,
    val emergencyAllowed: Boolean = false,
    val emergencyAt: Instant? = null,
    val error: EthernetInterfaceClientError? = null
)

enum class EthernetInterfaceClientError {
    Unknown
}

enum class EthernetConnectionStateType {
    Idle,
    Disconnected,
    Connected,
    Connecting,
    Emergency
}

data class EmergencyModeRequest(
    val enabled: Boolean
)