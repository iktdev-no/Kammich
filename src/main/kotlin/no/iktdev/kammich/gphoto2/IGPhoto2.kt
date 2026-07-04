package no.iktdev.kammich.gphoto2

import no.iktdev.kammich.gphoto2.model.GPhoto2Device
import no.iktdev.kammich.gphoto2.model.GPhoto2DeviceAbility
import no.iktdev.kammich.gphoto2.model.GPhoto2DiscoveredDevice
import no.iktdev.kammich.gphoto2.model.GPhoto2File
import no.iktdev.kammich.gphoto2.model.GPhoto2Summary
import java.io.File

interface IGPhoto2 {
    fun execute(vararg args: String): String
    fun discover(): List<GPhoto2DiscoveredDevice>
    fun getAbilities(device: GPhoto2DiscoveredDevice): GPhoto2DeviceAbility
    fun getSummary(device: GPhoto2DiscoveredDevice): GPhoto2Summary
    fun getDevices(): List<GPhoto2Device>
    fun copyFile(device: GPhoto2DiscoveredDevice, file: GPhoto2File, destination: File, onProgress: (Int) -> Unit)
    fun deleteFile(device: GPhoto2DiscoveredDevice, file: GPhoto2File): Boolean
    fun getThumbnail(cachePath: String, device: GPhoto2DiscoveredDevice, file: GPhoto2File): File
    fun getAbilities(port: String): GPhoto2DeviceAbility
    fun getSummary(port: String): GPhoto2Summary
    fun getDeviceInfo(port: String): GPhoto2Device
    fun getFiles(port: String, path: String? = null): List<GPhoto2File>
}