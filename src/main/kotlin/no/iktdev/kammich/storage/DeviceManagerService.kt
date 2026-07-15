package no.iktdev.kammich.storage

import no.iktdev.kammich.ConfigService
import no.iktdev.kammich.gphoto2.IGPhoto2
import no.iktdev.kammich.models.internal.DeviceReadyEvent
import no.iktdev.kammich.models.internal.SysPathRemoved
import no.iktdev.kammich.models.shared.DeviceSettingsDto
import no.iktdev.kammich.models.internal.config.DeviceSettings
import no.iktdev.kammich.models.shared.device.DeviceInfo
import no.iktdev.kammich.models.shared.device.GPhoto2Device
import no.iktdev.kammich.models.shared.device.RemovableDevice
import no.iktdev.kammich.repository.DeviceRepository
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.system.LsblkService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class DeviceManagerService(
    private val gPhoto2: IGPhoto2,
    private val lsblkService: LsblkService,
    private val sseManager: SseManager,
    private val deviceRepo: DeviceRepository,
    private val configService: ConfigService,
) {
    private val activeDevices = ConcurrentHashMap<String, RemovableDevice>()
    private val log = LoggerFactory.getLogger(DeviceManagerService::class.java)

    fun getActiveDevices(): List<RemovableDevice> {
        return activeDevices.values.toList()
    }

    fun getDevice(deviceId: String): RemovableDevice? {
        val active = activeDevices.values.find { it.id == deviceId }
        if (active == null) {
            log.info("Device not found: $deviceId in ${activeDevices.values.map { it.id }}")
        }
        return active
    }

    fun getDeviceBySysPath(sysPath: String): RemovableDevice? {
        return activeDevices[sysPath]
    }

    @EventListener(DeviceReadyEvent::class)
    fun onDeviceReady(event: DeviceReadyEvent) {
        val device = event.device
        activeDevices[device.sysPath] = device
        if (!hasSettings(device.id)) {
            updateConfig(device.id) {
                it.autoImport = (device is GPhoto2Device) &&
                        configService.getConfig().autoImportCameraByDefault
            }
        }
        log.info("Device $event was detected")
        deviceRepo.store(device)
        updateSSE()
    }

    @EventListener
    fun onDeviceRemoved(event: SysPathRemoved) {
        log.info("Device ${event.path} was removed")

        activeDevices.remove(event.path)
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

    fun setAutoImport(device: RemovableDevice, enabled: Boolean) {
        updateConfig(device.id) { it.autoImport = enabled }
    }

    fun setIncludeFolders(device: RemovableDevice, folders: List<String>) {
        updateConfig(device.id) { it.includeFolders = folders }
    }

    fun setExcludeFolders(device: RemovableDevice, folders: List<String>) {
        updateConfig(device.id) { it.excludeFolders = folders }
    }

    fun getDeviceInfo(id: String): DeviceInfo? {
        return getDevice(id)?.getRichInfo(gPhoto2, lsblkService, getSettings(id))
    }

}