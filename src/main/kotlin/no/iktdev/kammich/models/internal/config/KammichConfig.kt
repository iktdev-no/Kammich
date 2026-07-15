package no.iktdev.kammich.models.internal.config

import no.iktdev.kammich.models.immich.auth.ImmichAuth
import no.iktdev.kammich.models.shared.network.WifiSecurityType
import no.iktdev.kammich.models.shared.network.WifiTetherSetting

interface IKammichConfig {
    val mediaPath: String?
    val apiAuth: ImmichAuth?
    val assignUnknownDeviceAsBlockDevice: Boolean?
    val autoImportCameraByDefault: Boolean?
    val deviceSettings: MutableMap<String, DeviceSettings>?
    val tetherSetting: WifiTetherSetting?
    val tetherDevice: TetherDevice?
    val kammichHostpadPath: String?
}

data class RuntimeKammichConfig(
    override val mediaPath: String = "/var/lib/kammich/storage/media",
    override val apiAuth: ImmichAuth? = null,
    override val assignUnknownDeviceAsBlockDevice: Boolean = false,
    override val autoImportCameraByDefault: Boolean = true,
    override val deviceSettings: MutableMap<String, DeviceSettings> = mutableMapOf(),
    override val tetherSetting: WifiTetherSetting = WifiTetherSetting("Kammich", "kammich"),
    override val tetherDevice: TetherDevice? = null,
    override val kammichHostpadPath: String = "/run/kammich_aligned_ap.conf",
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
                tetherDevice = stored.tetherDevice ?: defaults.tetherDevice,
                kammichHostpadPath = stored.kammichHostpadPath ?: defaults.kammichHostpadPath,
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
    override val tetherSetting: WifiTetherSetting?,
    override val tetherDevice: TetherDevice?,
    override val kammichHostpadPath: String?
):  IKammichConfig


data class TetherDevice(
    val enabled: Boolean,
    val deviceId: String
)