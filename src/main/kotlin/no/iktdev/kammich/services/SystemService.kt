package no.iktdev.kammich.services

import no.iktdev.kammich.system.SysCommand
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
class SystemPowerService(
    private val exec: SysCommand
) {

    private val logger =
        Logger.getLogger(SystemPowerService::class.java.name)

    fun canPowerOff(): Boolean =
        exec
            .sudoCheck("/usr/bin/systemctl", "poweroff")
            .isSuccess()

    fun canReboot(): Boolean =
        exec
            .sudoCheck("/usr/bin/systemctl", "reboot")
            .isSuccess()

    fun executePowerOff(): Boolean {
        if (!canPowerOff()) {
            logger.warning(
                "Mangler sudo-rettigheter for poweroff."
            )
            return false
        }

        return exec
            .sudo(
                "/usr/bin/systemctl",
                "poweroff"
            )
            .isSuccess()
    }

    fun executeReboot(): Boolean {
        if (!canReboot()) {
            logger.warning(
                "Mangler sudo-rettigheter for reboot."
            )
            return false
        }

        return exec
            .sudo(
                "/usr/bin/systemctl",
                "reboot"
            )
            .isSuccess()
    }
}