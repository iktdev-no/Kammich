package no.iktdev.kammich.models.shared.network.old

import no.iktdev.kammich.models.shared.network.InterfaceActiveState


data class WifiConnectionResult(
    val success: Boolean,
    val message: String,
    val status: InterfaceActiveState
)