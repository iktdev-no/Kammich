package no.iktdev.kammich.models.shared.network

enum class CaptivePortalState {
    Online,          // 204 No Content - Alt er fryd og gammen
    CaptivePortal,  // 3xx Redirect - Fanget i felle (f.eks. på hotell eller café)
    Offline          // Ingen respons / feilet på alle sjekker
}

data class NetworkCaptiveStatus(
    val interfaceName: String,
    val state: CaptivePortalState,
    val portalUrl: String? = null,
    val message: String? = null
)