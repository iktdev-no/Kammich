package no.iktdev.kammich.system

import no.iktdev.kammich.models.shared.storage.Transport
import no.iktdev.kammich.models.shared.storage.LsblkBlockDevice
import no.iktdev.kammich.storage.parser.LsblkParser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LsblkService(
    private val exec: SysCommand
) {
    val parser = LsblkParser()
    private val log = LoggerFactory.getLogger(LsblkService::class.java)

    private fun getLsblkJson(): String? {
        return exec.nonSudo("lsblk", "--json", "-o", "NAME,PATH,MOUNTPOINT,MODEL,SERIAL,TYPE,TRAN,PTTYPE")
            .getOrNull()
    }

    fun getAllPhysicalDevices(vararg type: Transport): List<LsblkBlockDevice> {
        val out = exec.nonSudo("lsblk", "--json", "-o", "NAME,PATH,MOUNTPOINT,MODEL,SERIAL,TYPE,TRAN,PTTYPE")
        if (out is SysCommand.Result.Failure) {
            return emptyList()
        }
        val json = getLsblkJson() ?: run {
            log.error("Error getting lsblk json")
            return emptyList()
        }
        val devices = parser.getParentDevices(json)

        return if (type.isEmpty()) devices else devices.filter { it.transport in type }

    }

    fun getAllMountPoints(device: String): List<LsblkBlockDevice> {
        val out = exec.nonSudo("lsblk", device , "--json", "-o", "NAME,PATH,MOUNTPOINT,MODEL,SERIAL,TYPE,TRAN,PTTYPE")

        val json = out.getOrNull() ?: return emptyList()
        val devices = parser.getBlockDevices(json)

        return devices.filter { !it.mountPoint.isNullOrBlank() }
    }
}