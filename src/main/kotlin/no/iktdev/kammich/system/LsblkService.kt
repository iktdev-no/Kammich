package no.iktdev.kammich.system

import no.iktdev.kammich.models.shared.storage.Transport
import no.iktdev.kammich.models.shared.storage.LsblkBlockDevice
import no.iktdev.kammich.storage.parser.LsblkParser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LsblkService {
    val parser = LsblkParser()
    private val log = LoggerFactory.getLogger(LsblkService::class.java)

    fun getAllPhysicalDevices(vararg type: Transport): List<LsblkBlockDevice> {
        val process = ProcessBuilder(
            "lsblk", "--json", "-o", "NAME,PATH,MOUNTPOINT,MODEL,SERIAL,TYPE,TRAN,PTTYPE"
        ).start()

        val json = process.inputStream.bufferedReader().use { it.readText() }
        val devices = parser.getParentDevices(json)

        return if (type.isEmpty()) devices else devices.filter { it.transport in type }
    }

    fun getAllDevices(vararg type: Transport): List<LsblkBlockDevice> {
        val process = ProcessBuilder(
            "lsblk", "--json", "-o", "NAME,PATH,MOUNTPOINT,MODEL,SERIAL,TYPE,TRAN,PTTYPE"
        ).start()

        val json = process.inputStream.bufferedReader().use { it.readText() }
        val devices = parser.getBlockDevices(json)

        return if (type.isEmpty()) devices else devices.filter { it.transport in type }
    }

    fun getAllMountPoints(device: String): List<LsblkBlockDevice> {
        val process = ProcessBuilder(
            "lsblk", device , "--json", "-o", "NAME,PATH,MOUNTPOINT,MODEL,SERIAL,TYPE,TRAN,PTTYPE"
        ).start()

        val json = process.inputStream.bufferedReader().use { it.readText() }
        val devices = parser.getBlockDevices(json)

        return devices.filter { !it.mountPoint.isNullOrBlank() }
    }
}