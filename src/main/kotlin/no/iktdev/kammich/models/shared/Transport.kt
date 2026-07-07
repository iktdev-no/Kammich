package no.iktdev.kammich.models.shared

enum class Transport {
    USB, SATA, NVME, UNKNOWN;

    companion object {
        fun fromString(value: String?): Transport {
            return try {
                valueOf(value?.uppercase() ?: "UNKNOWN")
            } catch (e: IllegalArgumentException) {
                UNKNOWN
            }
        }
    }
}