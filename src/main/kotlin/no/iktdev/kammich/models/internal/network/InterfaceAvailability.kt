package no.iktdev.kammich.models.internal.network

import no.iktdev.kammich.models.shared.network.NetworkInterface
import no.iktdev.kammich.models.shared.network.NetworkInterfaceMode

data class InterfaceAvailability(
    val nif: NetworkInterface,
    val isAvailable: Boolean
)