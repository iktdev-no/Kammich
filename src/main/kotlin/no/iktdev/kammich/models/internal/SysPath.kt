package no.iktdev.kammich.models.internal

import java.time.ZonedDateTime

interface SysPath {
    val path: String
}

data class SysPathReady(
    override val path: String,
    val readyAt: ZonedDateTime = ZonedDateTime.now()
): SysPath

data class SysPathRemoved(
    override val path: String,
    val removedAt: ZonedDateTime = ZonedDateTime.now()
): SysPath