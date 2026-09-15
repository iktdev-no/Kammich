package no.iktdev.kammich.controller.networking

import no.iktdev.kammich.models.shared.tailscale.TailscaleDns
import no.iktdev.kammich.models.shared.tailscale.TailscaleNetcheck
import no.iktdev.kammich.models.shared.tailscale.TailscaleServe
import no.iktdev.kammich.models.shared.tailscale.TailscaleStatus
import no.iktdev.kammich.system.network.TailscaleService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/tailscale")
class TailscaleController(
    private val tailscaleService: TailscaleService
) {

    @GetMapping("/installed")
    fun isInstalled(): Boolean {
        return tailscaleService.isInstalled()
    }

    @GetMapping("/status")
    fun getStatus(): TailscaleStatus? {
        return tailscaleService.getStatus()
    }

    @GetMapping("/serve")
    fun getServing(): List<TailscaleServe> {
        return tailscaleService.getServe()
    }

    @GetMapping("/netcheck")
    fun getNetcheck(): ResponseEntity<TailscaleNetcheck> {
        val res = tailscaleService.getNetCheck()
        return if (res != null) {
            ResponseEntity.ok(res)
        } else ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @GetMapping("/dns")
    fun getDns(): ResponseEntity<TailscaleDns> {
        val res = tailscaleService.getDns()
        return if (res != null) {
            ResponseEntity.ok(res)
        } else ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}