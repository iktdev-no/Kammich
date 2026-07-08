package no.iktdev.kammich.storage

import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.gphoto2.IGPhoto2
import no.iktdev.kammich.gphoto2.model.GPhoto2Device
import no.iktdev.kammich.models.DeviceSettings
import no.iktdev.kammich.models.shared.DeviceSettingsDto
import no.iktdev.kammich.models.shared.storage.DeviceType
import no.iktdev.kammich.models.shared.storage.internal.BlockDeviceDetectedEvent
import no.iktdev.kammich.models.shared.storage.internal.DeviceDetectedEvent
import no.iktdev.kammich.models.shared.storage.internal.DeviceRemovedEvent
import no.iktdev.kammich.models.shared.storage.internal.MTPDeviceDetectedEvent
import no.iktdev.kammich.models.shared.storage.internal.PTPDeviceDetectedEvent
import no.iktdev.kammich.models.shared.storage.removable.Capability
import no.iktdev.kammich.models.shared.storage.removable.Device
import no.iktdev.kammich.models.shared.storage.removable.DeviceInfo
import no.iktdev.kammich.models.shared.storage.removable.DeviceStorageStats
import no.iktdev.kammich.repository.DeviceRepository
import no.iktdev.kammich.sse.SseManager
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Service
class DeviceManagerService(
    private val gPhoto2: IGPhoto2,
    private val sseManager: SseManager,
    private val deviceRepo: DeviceRepository,
    private val configService: ConfigService,
) {
    private val activeDevices = ConcurrentHashMap<String, Device>()
    private val log = LoggerFactory.getLogger(DeviceManagerService::class.java)

    fun getActiveDevices(): List<Device> {
        return activeDevices.values.toList()
    }

    fun getDevice(deviceId: String): Device? {
        val active = activeDevices.values.find { it.id == deviceId }
        if (active == null) {
            log.info("Device not found: $deviceId in ${activeDevices.values.map { it.id }}")
        }
        return active
    }

    fun getDeviceBySysPath(sysPath: String): Device? {
        return activeDevices[sysPath]
    }

    fun getDeviceById(id: String): Device? {
        return activeDevices.values.find { it.id == id }
    }

    fun getDeviceInfo(deviceId: String): DeviceInfo? {
        val device = getDeviceById(deviceId) ?: run {
            log.info("Device not found: $deviceId")
            return null
        }
        // Her er "hacker-stabilt" trikset:
        // Hvis det er et PTP-kamera, hent detaljer via GPhoto2-biblioteket
        // Hvis det er BLOCK, returner "hardkodet" info eller les fra disk-metadata
        return when (device.type) {
            DeviceType.PTP, DeviceType.MTP -> mapGPhoto2ToInfo(device)
            DeviceType.BLOCK -> mapBlockToInfo(device)
        }
    }

    private fun mapGPhoto2ToInfo(device: Device): DeviceInfo {
        val gd = gPhoto2.getDeviceInfo(device.path!!)

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
            attributes = attrs,
            deviceSettings = getSettings(device.id)
        )
    }

    private fun mapBlockToInfo(device: Device): DeviceInfo {
        // 1. Hent FileStore informasjon (krever at stien er montert)
        val file = java.io.File(device.path!!)
        val store = java.nio.file.Files.getFileStore(file.toPath())

        // 2. Map kapabiliteter for BlockDevice
        // En block device er stort sett en fil-container
        val caps = mutableListOf<Capability>()
        if (file.canWrite()) {
            caps.add(Capability.UPLOAD)
            caps.add(Capability.DELETE)
        }
        caps.add(Capability.PREVIEW) // Alle filer kan "forhåndsvises"

        // 3. Map lagringsenheter (Block devices har ofte bare én partisjon/store)
        val storage = listOf(
            DeviceStorageStats(
                id = "main",
                description = "Disk Partition",
                capacityBytes = store.totalSpace,
                freeSpaceBytes = store.usableSpace
            )
        )

        // 4. Attributes (OS-spesifikk info)
        val attrs = mutableMapOf<String, Any>()
        attrs["fileSystem"] = store.type()
        attrs["readOnly"] = file.canWrite().not()
        attrs["totalSpace"] = store.totalSpace

        return DeviceInfo(
            id = device.id,
            type = DeviceType.BLOCK, // Antar du har en BLOCK type
            friendlyName = device.name ?: "External Drive",
            manufacturer = "Generic", // Kan evt. hentes fra udev-info
            model = store.type(),
            capabilities = caps,
            storage = storage,
            attributes = attrs,
            deviceSettings = getSettings(device.id)
        )
    }

    @EventListener
    fun handleDeviceDetected(event: DeviceDetectedEvent) {

        // Hvis vi allerede har en enhet med denne ID-en, oppdaterer vi bare path
        // i stedet for å legge til en duplikat.
        val device = when (event) {
            is PTPDeviceDetectedEvent -> {
                val stableId = generateStableId(event.sysPath)
                val device = gPhoto2.getDeviceInfo(event.devicePath!!)
                val decodedDevice = device.toDevice(stableId, DeviceType.PTP)
                activeDevices[event.sysPath] = decodedDevice
                decodedDevice
            }

            is MTPDeviceDetectedEvent -> {
                val stableId = generateStableId(event.sysPath)
                val device = gPhoto2.getDeviceInfo(event.devicePath!!)
                val decodedDevice = device.toDevice(stableId, DeviceType.MTP)
                activeDevices[event.sysPath] = decodedDevice
                decodedDevice
            }

            is BlockDeviceDetectedEvent -> {
                log.info("Decoding DeviceDetected $event to blockDeviceDetectedEvent")
                val decodedDevice = event.toDevice()
                activeDevices[event.sysPath] = decodedDevice
                log.info("Assigned:\n${event.sysPath} to $decodedDevice")
                decodedDevice
            }
            else -> null
        }

        if (device != null) {
            val info = getDeviceInfo(event.sysPath)
            deviceRepo.store(device, info)
            if (!hasSettings(device.id)) {
                updateConfig(device.id) {
                    it.autoImport = (device.type != DeviceType.BLOCK) &&
                            configService.getConfig().autoImportCameraByDefault
                }
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
        val payload = ssePayload()
        log.info("Sender ${payload}")
        sseManager.send(payload)
    }

    private fun generateStableId(sysPath: String): String {
        // Les rådata fra sysfs - dette er stabilt uansett protokoll
        val vid = File("$sysPath/idVendor").takeIf { it.exists() }?.readText()?.trim() ?: "unknown"
        val pid = File("$sysPath/idProduct").takeIf { it.exists() }?.readText()?.trim() ?: "unknown"

        // Hvis vi har serienummer tilgjengelig, bruk det for å skille to like telefoner
        val serial = File("$sysPath/serial").takeIf { it.exists() }?.readText()?.trim() ?: ""

        return if (serial.isNotEmpty()) {
            "$serial"
        } else {
            "$vid:$pid"
        }
    }


    fun GPhoto2Device.toDevice(id: String, type: DeviceType): Device {
        return Device(
            id = id,                     // usb:001,004 → unikt nok
            name = "${summary.manufacturer} ${summary.model}",
            type = type,                              // PTP eller MTP
            path = this.connection,                   // gphoto2-port
            vendor = summary.manufacturer,
            model = summary.model
        )
    }

    fun BlockDeviceDetectedEvent.toDevice(): Device {
        return Device(
            id = this.defaultInfo.serial,
            name = this.defaultInfo.modelName,
            type = DeviceType.BLOCK,
            path = this.defaultInfo.mountPoint,
            vendor = this.vendor,
            model = this.defaultInfo.modelName
        )
    }

    private fun hasSettings(deviceId: String): Boolean {
        return configService.getConfig().deviceSettings.any { it.key == deviceId }
    }
    fun getSettings(deviceId: String): DeviceSettingsDto {
        val config = configService.getConfig()
        val settings = config.deviceSettings.getOrPut(deviceId) { DeviceSettings() }
        return settings.toDto()
    }

    private fun updateConfig(deviceId: String, block: (DeviceSettings) -> Unit) {
        val config = configService.getConfig()
        val settings = config.deviceSettings.getOrPut(deviceId) { DeviceSettings() }
        block(settings) // Her opererer vi nå direkte på backend-objektet
        configService.saveConfig(config)
    }

    fun updateDeviceSettings(deviceId: String, dto: DeviceSettingsDto) {
        updateConfig(deviceId) { settings ->
            settings.apply(dto) // Bruker apply-metoden vi lagde over
        }
    }

    fun setAutoImport(device: Device, enabled: Boolean) {
        updateConfig(device.id) { it.autoImport = enabled }
    }

    fun setIncludeFolders(device: Device, folders: List<String>) {
        updateConfig(device.id) { it.includeFolders = folders }
    }

    fun setExcludeFolders(device: Device, folders: List<String>) {
        updateConfig(device.id) { it.excludeFolders = folders }
    }

}