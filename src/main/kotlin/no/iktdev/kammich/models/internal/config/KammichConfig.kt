package no.iktdev.kammich.models.internal.config

import no.iktdev.kammich.models.immich.auth.ImmichAuth
import no.iktdev.kammich.models.shared.network.WifiTetherAP

interface IKammichConfig {
    val mediaPath: String?
    val apiAuth: ImmichAuth?
    val assignUnknownDeviceAsBlockDevice: Boolean?
    val autoImportCameraByDefault: Boolean?
    val deviceSettings: MutableMap<String, DeviceSettings>?
    val tetherSetting: WifiTetherAP?
    val selectedWirelessTetherInterface: SelectedWirelessTetherInterface?
}

data class RuntimeKammichConfig(
    override val mediaPath: String = "/var/lib/kammich/storage/media",
    override val apiAuth: ImmichAuth? = null,
    override val assignUnknownDeviceAsBlockDevice: Boolean = false,
    override val autoImportCameraByDefault: Boolean = true,
    override val deviceSettings: MutableMap<String, DeviceSettings> = mutableMapOf(),
    override val tetherSetting: WifiTetherAP = WifiTetherAP("Kammich", "kammich"),
    override val selectedWirelessTetherInterface: SelectedWirelessTetherInterface? = null,
): IKammichConfig {
    companion object {
        private val defaults = RuntimeKammichConfig()

        fun fromStored(stored: StoredKammichConfig): RuntimeKammichConfig {
            return RuntimeKammichConfig(
                mediaPath = stored.mediaPath ?: defaults.mediaPath,
                apiAuth = stored.apiAuth ?: defaults.apiAuth,
                assignUnknownDeviceAsBlockDevice = stored.assignUnknownDeviceAsBlockDevice ?: defaults.assignUnknownDeviceAsBlockDevice,
                autoImportCameraByDefault = stored.autoImportCameraByDefault ?: defaults.autoImportCameraByDefault,
                deviceSettings = stored.deviceSettings ?: defaults.deviceSettings,
                tetherSetting = stored.tetherSetting ?: defaults.tetherSetting,
                selectedWirelessTetherInterface = stored.selectedWirelessTetherInterface ?: defaults.selectedWirelessTetherInterface,
            )
        }
    }
}


data class StoredKammichConfig(
    override val mediaPath: String?,
    override val apiAuth: ImmichAuth?,
    override val assignUnknownDeviceAsBlockDevice: Boolean?,
    override val autoImportCameraByDefault: Boolean?,
    override val deviceSettings: MutableMap<String, DeviceSettings>?,
    override val tetherSetting: WifiTetherAP?,
    override val selectedWirelessTetherInterface: SelectedWirelessTetherInterface?,
):  IKammichConfig

data class SelectedWirelessTetherInterface(
    val autostart: Boolean,
    val deviceId: String,
)
