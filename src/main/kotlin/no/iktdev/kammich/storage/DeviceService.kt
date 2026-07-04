package no.iktdev.kammich.storage

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import no.iktdev.kammich.models.storage.Device
import org.springframework.stereotype.Service

@Service
class DeviceService {

    fun getAllDevices(): List<Device> {
        val process = ProcessBuilder(
            "lsblk", "--json", "-o", "NAME,PATH,MOUNTPOINT,MODEL,SERIAL,TYPE"
        ).start()

        val json = process.inputStream.bufferedReader().use { it.readText() }
        val root = JsonParser.parseString(json).asJsonObject
        val blockDevices = root.getAsJsonArray("blockdevices")

        val devices = mutableListOf<Device>()

        for (dev in blockDevices) {
            val obj = dev.asJsonObject

            val type = obj.get("type")?.asString ?: continue
            if (type != "disk") continue

            val path = obj.get("path")?.asString ?: continue
            val model = obj.get("model")?.asString ?: "Unknown"
            val serial = obj.get("serial")?.asString ?: "Unknown"

            val mountPoint = getMountPoint(obj)

            devices.add(
                Device(
                    path = path,
                    mountPoint = mountPoint,
                    serialNumber = serial,
                    modelName = model
                )
            )
        }

        return devices
    }

    private fun getMountPoint(obj: JsonObject): String {
        val mpElement = obj.get("mountpoint")

        if (mpElement != null && !mpElement.isJsonNull) {
            val mp = mpElement.asString
            if (mp.isNotBlank()) return mp
        }

        if (obj.has("children")) {
            val children = obj.getAsJsonArray("children")
            for (child in children) {
                val childObj = child.asJsonObject
                val childMpElement = childObj.get("mountpoint")

                if (childMpElement != null && !childMpElement.isJsonNull) {
                    val childMp = childMpElement.asString
                    if (childMp.isNotBlank()) {
                        return childMp
                    }
                }
            }
        }

        return ""
    }
}


