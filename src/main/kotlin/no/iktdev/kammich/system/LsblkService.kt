package no.iktdev.kammich.system

import no.iktdev.kammich.models.shared.storage.LsblkBlockDevice
import no.iktdev.kammich.models.shared.storage.Transport
import no.iktdev.kammich.storage.parser.LsblkParser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LsblkService(
    private val exec: SysCommand,
    private val parser: LsblkParser
) {
    private val log =
        LoggerFactory.getLogger(LsblkService::class.java)

    private val fields = listOf(
        "NAME",
        "PATH",
        "MOUNTPOINT",
        "MODEL",
        "SERIAL",
        "TYPE",
        "TRAN",
        "PTTYPE"
    )

    fun getAllPhysicalDevices(
        vararg transport: Transport
    ): List<LsblkBlockDevice> {
        val json = executeLsblk()
            ?: return emptyList()

        val devices = parser.getPhysicalDevices(json)

        if (transport.isEmpty()) {
            return devices
        }

        return devices.filter {
            it.transport in transport
        }
    }

    fun getAllMountPoints(
        device: String
    ): List<LsblkBlockDevice> {
        val json = executeLsblk(device)
            ?: return emptyList()

        return parser
            .getAllDevices(json)
            .filter {
                !it.mountPoint.isNullOrBlank()
            }
    }

    private fun executeLsblk(
        device: String? = null
    ): String? {
        val command = mutableListOf(
            "lsblk"
        )

        if (device != null) {
            command.add(device)
        }

        command.add("--json")
        command.add("-o")
        command.add(fields.joinToString(","))

        return exec
            .nonSudo(command)
            .fold(
                onSuccess = { output ->
                    output
                },
                onFailure = { output, error, exitCode ->
                    log.error(
                        "Failed to execute lsblk. " +
                                "exitCode={}, output={}, error={}",
                        exitCode,
                        output,
                        error
                    )

                    null
                }
            )
    }
}