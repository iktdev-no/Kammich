package no.iktdev.kammich.services

import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

@Service
class SystemPowerService {

    private val logger = Logger.getLogger(SystemPowerService::class.java.name)

    private fun checkPermission(vararg command: String): Boolean {
        return try {
            val process = ProcessBuilder(
                listOf("sudo", "-n", "-l") + command
            )
                .redirectErrorStream(true)
                .start()

            val finished = process.waitFor(2, TimeUnit.SECONDS)

            if (!finished) {
                process.destroyForcibly()
                return false
            }

            process.exitValue() == 0
        } catch (e: Exception) {
            logger.warning("Kunne ikke sjekke sudo-rettigheter: ${e.message}")
            false
        }
    }

    fun canPowerOff(): Boolean =
        checkPermission("/usr/bin/systemctl", "poweroff")

    fun canReboot(): Boolean =
        checkPermission("/usr/bin/systemctl", "reboot")

    fun executePowerOff(): Boolean {
        if (!canPowerOff()) {
            logger.warning("Mangler sudo-rettigheter for poweroff.")
            return false
        }

        return try {
            ProcessBuilder(
                "sudo",
                "-n",
                "/usr/bin/systemctl",
                "poweroff"
            ).start()

            true
        } catch (e: Exception) {
            logger.severe("Klarte ikke å utføre poweroff: ${e.message}")
            false
        }
    }

    fun executeReboot(): Boolean {
        if (!canReboot()) {
            logger.warning("Mangler sudo-rettigheter for reboot.")
            return false
        }

        return try {
            ProcessBuilder(
                "sudo",
                "-n",
                "/usr/bin/systemctl",
                "reboot"
            ).start()

            true
        } catch (e: Exception) {
            logger.severe("Klarte ikke å utføre reboot: ${e.message}")
            false
        }
    }
}