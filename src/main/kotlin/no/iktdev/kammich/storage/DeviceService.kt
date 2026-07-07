package no.iktdev.kammich.storage

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import no.iktdev.kammich.models.shared.Transport
import no.iktdev.kammich.models.shared.storage.BlockDevice
import no.iktdev.kammich.storage.parser.DiskInfoParser
import org.springframework.stereotype.Service

@Service
class DeviceService {
    val parser = DiskInfoParser()

    fun getAllDevices(vararg type: Transport): List<BlockDevice> {
        val process = ProcessBuilder(
            "lsblk", "--json", "-o", "NAME,PATH,MOUNTPOINT,MODEL,SERIAL,TYPE,TRAN,PTTYPE"
        ).start()

        val json = process.inputStream.bufferedReader().use { it.readText() }
        val devices = parser.getBlockDevices(json)

        return devices.filter { it.transport in type }
    }

    fun getAllMountPoints(device: String): List<BlockDevice> {
        val process = ProcessBuilder(
            "lsblk", device , "--json", "-o", "NAME,PATH,MOUNTPOINT,MODEL,SERIAL,TYPE,TRAN,PTTYPE"
        ).start()

        val json = process.inputStream.bufferedReader().use { it.readText() }
        val devices = parser.getBlockDevices(json)

        return devices.filter { !it.mountPoint.isNullOrBlank() }
    }
}


