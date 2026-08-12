package no.iktdev.kammich.models.internal.events

import no.iktdev.kammich.models.shared.device.RemovableDevice
import java.time.ZonedDateTime

data class DeviceReadyEvent(val device: RemovableDevice, val readyAt: ZonedDateTime = ZonedDateTime.now())