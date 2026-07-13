package no.iktdev.kammich.models.shared.network

data class WifiSseEvent(
    val status: WifiConnectivityState,
    val networks: List<WifiNetwork> = emptyList(),
    val errorMessage: String? = null
)