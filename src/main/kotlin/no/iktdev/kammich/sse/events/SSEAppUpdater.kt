package no.iktdev.kammich.sse.events

import no.iktdev.kammich.models.shared.update.AppUpdateProgress
import no.iktdev.kammich.sse.ISSE

data class SSEAppUpdater(
    val payload: AppUpdateProgress
) : ISSE {
    override val type = "app-updater"
}