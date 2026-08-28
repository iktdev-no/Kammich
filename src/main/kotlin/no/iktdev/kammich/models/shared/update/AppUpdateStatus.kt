package no.iktdev.kammich.models.shared.update

enum class AppUpdateStatus {
    None,
    Checking,
    UpdateAvailable,
    Downloading,
    Verifying,
    Replacing,
    Restarting,
    Failed
}