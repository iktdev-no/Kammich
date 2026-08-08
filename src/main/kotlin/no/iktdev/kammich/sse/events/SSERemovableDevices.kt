package no.iktdev.kammich.sse.events

import no.iktdev.kammich.models.shared.device.RemovableDevice
import no.iktdev.kammich.sse.ISSE

data class SSERemovableDevices(val payload: List<RemovableDevice>): ISSE {
    override val type: String = "removable-devices"
}