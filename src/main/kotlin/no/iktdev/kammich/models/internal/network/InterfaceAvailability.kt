package no.iktdev.kammich.models.internal.network

import no.iktdev.kammich.models.shared.network.NetworkInterface

data class InterfaceAvailability(
    val nif: NetworkInterface,
    val state: InterfaceState?,
    val isAvailable: Boolean
)