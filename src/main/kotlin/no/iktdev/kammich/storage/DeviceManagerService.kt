package no.iktdev.kammich.storage

import com.google.gson.Gson
import no.iktdev.kammich.gphoto2.IGPhoto2
import no.iktdev.kammich.gphoto2.model.GPhoto2Device
import no.iktdev.kammich.models.files.KFile
import no.iktdev.kammich.models.storage.DeviceType
import no.iktdev.kammich.models.storage.internal.BlockDeviceDetectedEvent
import no.iktdev.kammich.models.storage.internal.DeviceDetectedEvent
import no.iktdev.kammich.models.storage.internal.DeviceRemovedEvent
import no.iktdev.kammich.models.storage.internal.MTPDeviceDetectedEvent
import no.iktdev.kammich.models.storage.internal.PTPDeviceDetectedEvent
import no.iktdev.kammich.models.storage.removable.Capability
import no.iktdev.kammich.models.storage.removable.Device
import no.iktdev.kammich.models.storage.removable.DeviceInfo
import no.iktdev.kammich.models.storage.removable.DeviceStorageStats
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.storage.provider.StorageProviderFactory
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Service
class DeviceManagerService(
    private val gPhoto2: IGPhoto2,
    private val sseManager: SseManager,
    private val providerFactory: StorageProviderFactory
) {
    private val activeDevices = ConcurrentHashMap<String, Device>()
    private val log = LoggerFactory.getLogger(DeviceManagerService::class.java)

    fun getActiveDevices(): List<Device> {
        return activeDevices.values.toList()
    }

    fun getDevice(deviceId: String): Device? {
        return activeDevices.values.find { it.id == deviceId }
    }

    fun getDeviceInfo(deviceId: String): DeviceInfo? {
        val device = getDevice(deviceId) ?: return null
        val provider = providerFactory.getProvider(device)

        // Her er "hacker-stabilt" trikset:
        // Hvis det er et PTP-kamera, hent detaljer via GPhoto2-biblioteket
        // Hvis det er BLOCK, returner "hardkodet" info eller les fra disk-metadata
        return when (device.type) {
            DeviceType.PTP, DeviceType.MTP -> mapGPhoto2ToInfo(device)
            DeviceType.BLOCK -> mapBlockToInfo(device)
        }
    }

    private fun mapGPhoto2ToInfo(device: Device): DeviceInfo {
        val gd = gPhoto2.getDeviceInfo(device.id)

        // 1. Map kapabiliteter basert på GPhoto2-logikk
        val caps = mutableListOf<Capability>()
        if (gd.ability.captureChoices.isNotEmpty()) caps.add(Capability.CAPTURE)
        if (gd.ability.deleteSelectedFiles || gd.ability.deleteAllFiles) caps.add(Capability.DELETE)
        if (gd.ability.fileUploadSupport) caps.add(Capability.UPLOAD)
        if (gd.ability.filePreviewSupport) caps.add(Capability.PREVIEW)
        if (gd.ability.configurationSupport) caps.add(Capability.CONFIGURE)

        // 2. Map lagringsenheter
        val storage = gd.summary.storageDevices.map {
            DeviceStorageStats(
                id = it.id,
                description = it.description,
                capacityBytes = it.capacityBytes,
                freeSpaceBytes = it.freeSpaceBytes
            )
        }

        // 3. Pakk inn "ekstra" info i attributes
        val attrs = mutableMapOf<String, Any>()
        gd.summary.batteryLevel?.let { attrs["batteryLevel"] = it }
        gd.summary.serialNumber?.let { attrs["serialNumber"] = it }
        attrs["usbSupport"] = gd.ability.usbSupport
        attrs["serialPortSupport"] = gd.ability.serialPortSupport

        // 4. Returner ferdig mappet objekt
        return DeviceInfo(
            id = device.id,
            type = DeviceType.PTP,
            friendlyName = gd.summary.friendlyDeviceName ?: device.name,
            manufacturer = gd.summary.manufacturer,
            model = gd.summary.model,
            capabilities = caps,
            storage = storage,
            attributes = attrs
        )
    }

    private fun mapBlockToInfo(device: Device): DeviceInfo {
        TODO("Not yet implemented")
    }

    fun getFilesForDevice(device: Device, path: String): List<KFile> {
        log.info("Getting files for device ${device.id} on path $path")
        val provider = providerFactory.getProvider(device)
        return provider.listFiles(device, path)
    }

    fun getThumbnailsForFolder(folder: KFile): List<File> {
        val provider = providerFactory.getProvider(folder.device)
        return provider.getThumbnails(folder)
    }

    @EventListener
    fun handleDeviceDetected(event: DeviceDetectedEvent) {
        when (event) {
            is PTPDeviceDetectedEvent -> {
                val device = gPhoto2.getDeviceInfo(event.devicePath!!)
                activeDevices[event.sysPath] = device.toDevice(DeviceType.PTP)
            } is MTPDeviceDetectedEvent -> {
            val device = gPhoto2.getDeviceInfo(event.devicePath!!)
            activeDevices[event.sysPath] = device.toDevice(DeviceType.MTP)
            }
            is BlockDeviceDetectedEvent -> {

            }
        }

        log.info("Device ${event.devicePath} was detected")
        updateSSE()
    }

    @EventListener
    fun handleDeviceRemoved(event: DeviceRemovedEvent) {
        log.info("Device ${event.sysPath} was removed")

        activeDevices.remove(event.sysPath)
        updateSSE()
    }

    fun ssePayload(): Map<String, Any> {
        return mapOf(
            "type" to "removable-devices",
            "payload" to activeDevices.values.toList()
        )
    }

    fun updateSSE() {
        log.info("Sender ${Gson().toJson(activeDevices)}")
        sseManager.send(ssePayload())
    }


    fun GPhoto2Device.toDevice(type: DeviceType): Device {
        return Device(
            id = this.connection,                     // usb:001,004 → unikt nok
            name = "${summary.manufacturer} ${summary.model}",
            type = type,                              // PTP eller MTP
            path = this.connection,                   // gphoto2-port
            vendor = summary.manufacturer,
            model = summary.model
        )
    }

}