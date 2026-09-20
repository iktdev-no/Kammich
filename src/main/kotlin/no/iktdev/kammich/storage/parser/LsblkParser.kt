package no.iktdev.kammich.storage.parser

import com.google.gson.Gson
import no.iktdev.kammich.models.internal.LsblkDevice
import no.iktdev.kammich.models.internal.LsblkResponse
import no.iktdev.kammich.models.shared.storage.LsblkBlockDevice
import no.iktdev.kammich.models.shared.storage.Transport
import org.springframework.stereotype.Component

@Component
class LsblkParser {

    private val gson = Gson()

    fun getPhysicalDevices(
        json: String
    ): List<LsblkBlockDevice> {
        val response = gson.fromJson(
            json,
            LsblkResponse::class.java
        )

        return response.devices.map { device ->
            createBlockDevice(
                device = device,
                modelName = device.model ?: "Unknown",
                serialNumber = device.serial ?: "Unknown",
                transport = Transport.fromString(
                    device.transport
                )
            )
        }
    }

    fun getAllDevices(
        json: String
    ): List<LsblkBlockDevice> {
        val response = gson.fromJson(
            json,
            LsblkResponse::class.java
        )

        val devices = mutableListOf<LsblkBlockDevice>()

        response.devices.forEach { device ->
            flatten(
                device = device,
                modelName = device.model ?: "Unknown",
                serialNumber = device.serial ?: "Unknown",
                transport = Transport.fromString(
                    device.transport
                ),
                output = devices
            )
        }

        return devices
    }

    private fun flatten(
        device: LsblkDevice,
        modelName: String,
        serialNumber: String,
        transport: Transport,
        output: MutableList<LsblkBlockDevice>
    ) {
        output.add(
            createBlockDevice(
                device = device,
                modelName = modelName,
                serialNumber = serialNumber,
                transport = transport
            )
        )

        device.children?.forEach { child ->
            flatten(
                device = child,
                modelName = modelName,
                serialNumber = serialNumber,
                transport = transport,
                output = output
            )
        }
    }

    private fun createBlockDevice(
        device: LsblkDevice,
        modelName: String,
        serialNumber: String,
        transport: Transport
    ): LsblkBlockDevice {
        return LsblkBlockDevice(
            name = device.name,
            path = device.path ?: "/dev/${device.name}",
            mountPoint = device.mountpoint,
            serialNumber = serialNumber,
            modelName = modelName,
            transport = transport,
            mounted = !device.mountpoint.isNullOrBlank()
        )
    }
}