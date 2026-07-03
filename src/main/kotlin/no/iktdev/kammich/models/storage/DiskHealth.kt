package no.iktdev.kammich.models.storage

import com.google.gson.annotations.SerializedName

data class DiskHealth(
    val deviceName: String,     // f.eks. /dev/sda
    val modelName: String,      // f.eks. SAMSUNG SSD 830 Series
    val serialNumber: String,   // f.eks. S0VYNYABB00655
    val protocol: String,       // f.eks. SATA eller NVMe
    val isHealthy: Boolean,
    val percentageUsed: Int,
    val temperatureCelsius: Int
)


// Felles rot-elementer finnes i begge
open class SmartCtlRoot(
    @SerializedName("model_name") val modelName: String,
    @SerializedName("serial_number") val serialNumber: String,
    @SerializedName("smart_status") val smartStatus: SmartStatus
)

data class NvmeRoot(
    @SerializedName("nvme_smart_health_information_log") val log: NvmeLog
) : SmartCtlRoot("", "", SmartStatus(false))

data class SataRoot(
    @SerializedName("ata_smart_attributes") val attrs: SataAttributes
) : SmartCtlRoot("", "", SmartStatus(false))

// Støtteklasser
data class SmartStatus(val passed: Boolean)
data class NvmeLog(
    @SerializedName("percentage_used") val pUsed: Int,
    @SerializedName("temperature") val temp: Int
)
data class SataAttributes(val table: List<SataAttribute>)
data class SataAttribute(val name: String, val raw: RawValue)
data class RawValue(@SerializedName("string") val value: String)