package no.iktdev.kammich.models.shared.network

enum class ConnectivityState {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    CAPTIVE_PORTAL,
    FAILED,
    ERROR
}