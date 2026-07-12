package no.iktdev.kammich.storage.parser

import com.google.gson.Gson
import no.iktdev.kammich.models.shared.Transport
import no.iktdev.kammich.models.shared.storage.LsblkBlockDevice
import no.iktdev.kammich.models.LsblkResponse

class LsblkParser {
    private val gson = Gson()

    fun getParentDevices(json: String): List<LsblkBlockDevice> {
        val response = gson.fromJson(json, LsblkResponse::class.java)
        val devices = mutableListOf<LsblkBlockDevice>()

        for (disk in response.devices) {
            val model = disk.model ?: "Unknown"
            val serial = disk.serial ?: "Unknown"
            val transport = Transport.fromString(disk.transport)

            // Vi legger til disken selv om den ikke er montert
            // Vi bruker "Not mounted" som fallback-verdi
            devices.add(createBlockDevice(
                name = disk.name,
                path = disk.path ?: "/dev/${disk.name}",
                mountPoint = disk.mountpoint ?: "Not mounted",
                serialNumber = serial,
                modelName = model,
                transport = transport,
                mounted = !disk.mountpoint.isNullOrBlank(),
            ))
        }
        return devices
    }

    fun getBlockDevices(json: String): List<LsblkBlockDevice> {
        val response = gson.fromJson(json, LsblkResponse::class.java)
        val devices = mutableListOf<LsblkBlockDevice>()

        for (disk in response.devices) {
            val model = disk.model ?: "Unknown"
            val serial = disk.serial ?: "Unknown"
            val transport = Transport.fromString(disk.transport)

            // Vi legger til disken selv om den ikke er montert
            // Vi bruker "Not mounted" som fallback-verdi
            devices.add(createBlockDevice(
                name = disk.name,
                path = disk.path ?: "/dev/${disk.name}",
                mountPoint = disk.mountpoint ?: "Not mounted",
                serialNumber = serial,
                modelName = model,
                transport = transport,
                mounted = !disk.mountpoint.isNullOrBlank(),
            ))

            // Vi går gjennom partisjonene og legger til alle, uansett om de er montert
            disk.children?.forEach { partition ->
                devices.add(
                    createBlockDevice(
                        name = partition.name,
                        path = partition.path ?: "/dev/${partition.name}",
                        mountPoint = partition.mountpoint ?: "Not mounted",
                        serialNumber = serial,
                        modelName = model,
                        transport = transport,
                        mounted = !partition.mountpoint.isNullOrBlank()
                    )
                )
            }
        }
        return devices
    }

    private fun createBlockDevice(
        name: String,
        path: String?,
        mountPoint: String,
        mounted: Boolean,
        serialNumber: String,
        modelName: String,
        transport: Transport
    ): LsblkBlockDevice {
        return LsblkBlockDevice(
            name = name,
            path = path ?: "Unknown",
            mountPoint = mountPoint,
            serialNumber = serialNumber,
            modelName = modelName,
            transport = transport,
            mounted = mounted
        )
    }
}