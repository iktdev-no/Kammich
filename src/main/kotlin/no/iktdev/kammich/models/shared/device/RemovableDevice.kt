package no.iktdev.kammich.models.shared.device

import no.iktdev.kammich.gphoto2.IGPhoto2
import no.iktdev.kammich.gphoto2.model.GPhoto2StorageDevice
import no.iktdev.kammich.models.shared.DeviceSettingsDto
import no.iktdev.kammich.system.LsblkService
import no.iktdev.kammich.toCaps

interface RemovableDevice {
    val id: String
    val name: String
    val model: String
    val manufacturer: String
    val sn: String
    val type: DeviceType
    val sysPath: String

    fun getRichInfo(gPhoto2: IGPhoto2, lsblkService: LsblkService, settings: DeviceSettingsDto?): DeviceInfo
}

data class GPhoto2Device(
    override val id: String, // Serialnumber if present, if not, stable id
    override val name: String,
    override val model: String,
    override val manufacturer: String,
    override val sn: String,
    val port: String,
    override val type: DeviceType,
    override val sysPath: String,
    val storage: List<GPhoto2StorageDevice>
): RemovableDevice {
    override fun getRichInfo(gPhoto2: IGPhoto2, lsblkService: LsblkService, settings: DeviceSettingsDto?): DeviceInfo {
        val gd = gPhoto2.getDeviceInfo(port)
        return DeviceInfo(
            id = id,
            type = type,
            friendlyName = gd.summary.friendlyDeviceName ?: name,
            manufacturer = gd.summary.manufacturer ?: manufacturer,
            model = gd.summary.model ?: model,
            capabilities = gd.ability.toCaps(),
            storage = gd.summary.storageDevices.map {
                DeviceStorageStats(
                    id = it.id,
                    description = it.description,
                    capacityBytes = it.capacityBytes,
                    freeSpaceBytes = it.freeSpaceBytes
                )
            },
            attributes = mapOf("batteryLevel" to (gd.summary.batteryLevel ?: -1)),
            deviceSettings = settings
        )
    }
}

data class BlockDevice(
    override val id: String, // Will be serial number
    override val name: String,
    override val model: String,
    override val manufacturer: String,
    override val sn: String,
    val mountPoint: String?,
    override val type: DeviceType = DeviceType.BLOCK,
    override val sysPath: String,
    val devicePath: String
): RemovableDevice {
    override fun getRichInfo(gPhoto2: IGPhoto2, lsblkService: LsblkService, settings: DeviceSettingsDto?): DeviceInfo {
        // 1. Definer standard-objekt hvis disken ikke er montert
        val file = mountPoint?.let { java.io.File(it) }
        val isMounted = file?.exists() == true

        // 2. Hent lagringsinfo hvis mulig, ellers tomme verdier
        val store = if (isMounted) runCatching { java.nio.file.Files.getFileStore(file!!.toPath()) }.getOrNull() else null

        // 3. Kapabiliteter: Vi kan alltid forhåndsvise, men skriving krever mount + skrivetilgang
        val caps = listOfNotNull(
            Capability.PREVIEW,
            Capability.UPLOAD.takeIf { isMounted && file!!.canWrite() },
            Capability.DELETE.takeIf { isMounted && file!!.canWrite() }
        )

        // 4. Lagringsstats
        val storage = if (store != null) {
            listOf(DeviceStorageStats(
                id = "main",
                description = "Disk Partition ($mountPoint)",
                capacityBytes = store.totalSpace,
                freeSpaceBytes = store.usableSpace
            ))
        } else emptyList()

        // 5. Attributter
        val attrs = mutableMapOf<String, Any>(
            "mounted" to isMounted,
            "mountPoint" to (mountPoint ?: "Not mounted")
        )
        store?.let {
            attrs["fileSystem"] = it.type()
            attrs["totalSpace"] = it.totalSpace
        }

        return DeviceInfo(
            id = id,
            type = type,
            friendlyName = name,
            manufacturer = manufacturer,
            model = model,
            capabilities = caps,
            storage = storage,
            attributes = attrs,
            deviceSettings = settings
        )
    }
}

data class DeviceInfo(
    val id: String,
    val type: DeviceType,
    val friendlyName: String?,
    val manufacturer: String?,
    val model: String?,
    val capabilities: List<Capability>, // Liste fremfor bool-felter
    val storage: List<DeviceStorageStats>,
    val attributes: Map<String, Any> = emptyMap(), // "Alt det andre"
    val deviceSettings: DeviceSettingsDto? = null,
)

enum class Capability {
    CAPTURE, DELETE, UPLOAD, PREVIEW, CONFIGURE
}

data class DeviceStorageStats(
    val id: String,
    val description: String,
    val capacityBytes: Long,
    val freeSpaceBytes: Long
)

