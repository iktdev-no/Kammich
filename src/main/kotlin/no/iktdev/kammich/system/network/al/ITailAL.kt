package no.iktdev.kammich.system.network.al

import no.iktdev.kammich.models.shared.tailscale.TailscaleDns
import no.iktdev.kammich.models.shared.tailscale.TailscaleNetcheck
import no.iktdev.kammich.models.shared.tailscale.TailscaleServe
import no.iktdev.kammich.models.shared.tailscale.TailscaleStatus

interface ITailAL {
    fun getServe(): List<TailscaleServe>

    fun getNetcheck(): TailscaleNetcheck?
    fun getDns(): TailscaleDns?
    fun getStatus(): TailscaleStatus?
    fun isSupported(): Boolean
}