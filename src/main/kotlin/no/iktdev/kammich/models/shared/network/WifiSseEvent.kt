package no.iktdev.kammich.models.shared.network

data class WifiSseEvent(
    val status: WifiActivityState,
    val networks: List<FeWifiNetwork> = emptyList(),
    val errorMessage: String? = null
)