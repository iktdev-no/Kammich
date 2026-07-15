package no.iktdev.kammich.models.shared.network

data class WifiSseEvent(
    val status: ConnectivityState,
    val networks: List<WifiNetwork> = emptyList(),
    val errorMessage: String? = null
)