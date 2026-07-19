package no.iktdev.kammich.models.shared.network

enum class InterfaceActiveState {
    Idle,
    Scanning,
    StartingTether,
    Tethering,
    StoppingTether,
    Connecting,
    Connected,
    Disconnected,
    CaptivePortal,
}