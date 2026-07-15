package no.iktdev.kammich.system.network.wifi

import no.iktdev.kammich.models.shared.network.ConnectivityState
import no.iktdev.kammich.models.shared.network.WifiNetwork
import no.iktdev.kammich.models.shared.network.WifiScanState
import no.iktdev.kammich.models.shared.network.WifiTethering
import no.iktdev.kammich.models.shared.network.WifiTetheringNetwork
import no.iktdev.kammich.models.shared.network.WifiTetheringState
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@Component
class WifiStateRegistry {

    val scanLastScans = ConcurrentHashMap<String, ZonedDateTime>()
    val scanResults = ConcurrentHashMap<String, List<WifiNetwork>>()
    val scanCurrentStates = ConcurrentHashMap<String, WifiScanState>()

    val connectivityCurrentStates = ConcurrentHashMap<String, ConnectivityState>()
    val connectivityCurrentNetworks = ConcurrentHashMap<String, WifiNetwork>()

    @OptIn(ExperimentalAtomicApi::class)
    val tetheringCurrentTether = AtomicReference<WifiTethering?>(null)


}