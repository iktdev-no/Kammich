package no.iktdev.kammich.storage.parser

import com.google.gson.Gson
import no.iktdev.kammich.models.shared.Transport
import no.iktdev.kammich.models.shared.storage.BlockDevice
import no.iktdev.kammich.models.LsblkResponse

class DiskInfoParser {
    private val gson = Gson()

    fun getBlockDevices(json: String): List<BlockDevice> {
        val response = gson.fromJson(json, LsblkResponse::class.java)
        val devices = mutableListOf<BlockDevice>()

        for (disk in response.devices) {
            // Vi henter metadata fra disken
            val model = disk.model ?: "Unknown"
            val serial = disk.serial ?: "Unknown"
            val transport = Transport.fromString(disk.transport)

            // Vi sjekker om disken selv er mountet
            if (!disk.mountpoint.isNullOrBlank()) {
                devices.add(createBlockDevice(disk.name, disk.path, disk.mountpoint, serial, model, transport))
            }

            // Vi går gjennom partisjonene (children) og henter deres mountpoints
            disk.children?.forEach { partition ->
                if (!partition.mountpoint.isNullOrBlank()) {
                    devices.add(
                        createBlockDevice(
                            name = partition.name,
                            path = partition.path ?: "/dev/${partition.name}",
                            mountPoint = partition.mountpoint,
                            serialNumber = serial, // Arver fra disk
                            modelName = model,     // Arver fra disk
                            transport = transport  // Arver fra disk
                        )
                    )
                }
            }
        }

        return devices
    }

    private fun createBlockDevice(
        name: String,
        path: String?,
        mountPoint: String,
        serialNumber: String,
        modelName: String,
        transport: Transport
    ): BlockDevice {
        return BlockDevice(
            name = name,
            path = path ?: "Unknown",
            mountPoint = mountPoint,
            serialNumber = serialNumber,
            modelName = modelName,
            transport = transport
        )
    }
}