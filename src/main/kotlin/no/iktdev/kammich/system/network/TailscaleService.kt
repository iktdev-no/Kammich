package no.iktdev.kammich.system.network

import no.iktdev.kammich.models.shared.tailscale.TailscaleDns
import no.iktdev.kammich.models.shared.tailscale.TailscaleDnsServer
import no.iktdev.kammich.models.shared.tailscale.TailscaleNetcheck
import no.iktdev.kammich.models.shared.tailscale.TailscaleServe
import no.iktdev.kammich.models.shared.tailscale.TailscaleStatus
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.system.network.al.ITailAL
import no.iktdev.kammich.system.network.al.TailAL
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class TailscaleService(
    private val sseManager: SseManager,
    private val eventPublisher: ApplicationEventPublisher,
    private val tailAL: ITailAL
) {

    fun getStatus(): TailscaleStatus? {
        return tailAL.getStatus()
    }

    fun getServe(): List<TailscaleServe> {
        return tailAL.getServe()
    }

    fun getNetCheck(): TailscaleNetcheck? {
        return tailAL.getNetcheck()
    }

    fun getDns(): TailscaleDns? {
        return tailAL.getDns()
    }

    fun isInstalled(): Boolean {
        return tailAL.isSupported()
    }

}