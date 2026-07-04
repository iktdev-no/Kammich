package no.iktdev.kammich.models.storage.internal

abstract class DeviceEvent(
    open val sysPath: String,
) {
}

data class DeviceRemovedEvent(
    override val sysPath: String,
): DeviceEvent(sysPath)


abstract class DeviceDetectedEvent(
    override val sysPath: String,
    open val vendor: String,
    open val product: String,
    open val serial: String,
    open val devicePath: String?,
): DeviceEvent(sysPath)


data class PTPDeviceDetectedEvent(
    override val sysPath: String,
    override val vendor: String,
    override val product: String,
    override val serial: String,
    override val devicePath: String?,
): DeviceDetectedEvent(sysPath, vendor, product, serial, devicePath)
data class MTPDeviceDetectedEvent(
    override val sysPath: String,
    override val vendor: String,
    override val product: String,
    override val serial: String,
    override val devicePath: String,
): DeviceDetectedEvent(sysPath, vendor, product, serial, devicePath)
data class BlockDeviceDetectedEvent(
    override val sysPath: String,
    override val vendor: String,
    override val product: String,
    override val serial: String,
    override val devicePath: String,
): DeviceDetectedEvent(sysPath, vendor, product, serial, devicePath)