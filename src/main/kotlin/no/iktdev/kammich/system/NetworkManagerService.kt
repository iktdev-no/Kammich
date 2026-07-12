package no.iktdev.kammich.system

import no.iktdev.kammich.system.network.GatewayController
import no.iktdev.kammich.system.network.InternetChecker
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class NetworkManagerService(
    private val gwc: GatewayController,
    private val internetChecker: InternetChecker
) {

    @Scheduled(fixedDelay = 10000)
    fun watchDog() {
        if (!internetChecker.isInternetReachable()) {
            // Her vet vi at vi er "offline", så vi tvinger opp AP-en
            gwc.ensureGatewayRunning()
        }
    }
}