package no.iktdev.kammich.gphoto2.model

data class GPhoto2Device(
    val connection: GPhoto2DiscoveredDevice,
    val ability: GPhoto2DeviceAbility,
    val summary: GPhoto2Summary
)