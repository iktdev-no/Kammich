package no.iktdev.kammich.models.shared.network

data class WifiScanStatus(val ifName: String, val isScanning: Boolean)
data class WifiScanResult(val ifName: String,
                          val networks: List<WifiNetwork>,
                          val error: WifiScanError? = null
)

enum class WifiScanError {
    Unknown
}