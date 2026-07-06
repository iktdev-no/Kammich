package no.iktdev.kammich.storage.internal

import no.iktdev.kammich.models.shared.storage.DiskHealth
import no.iktdev.kammich.models.shared.storage.SataAttribute
import no.iktdev.kammich.models.shared.storage.SataRoot

enum class DiskVendor {
    SAMSUNG,
    INTEL,
    CRUCIAL,
    WD_SANDISK,
    GENERIC;

    companion object {
        fun fromModel(model: String): DiskVendor {
            val m = model.lowercase()
            return when {
                m.contains("samsung") -> SAMSUNG
                m.contains("intel") -> INTEL
                m.contains("crucial") -> CRUCIAL
                m.contains("sandisk") || m.contains("wd") -> WD_SANDISK
                else -> GENERIC
            }
        }
    }
}

data class ParsedSataData(
    val normalizedWear: Int?,
    val rawWear: Int?,
    val temperature: Int,
    val vendor: DiskVendor
)

object DiskVendorParsing {

    fun parseCommon(root: SataRoot): ParsedSataData {
        val table = root.attrs.table

        val wearAttr: SataAttribute? = table.find { it.name.contains("Wear", ignoreCase = true) }
        val normalizedWear = wearAttr?.value
        val rawWear = wearAttr?.raw?.value?.toInt()

        val tempString = table.find { it.name.contains("Temp", ignoreCase = true) }?.raw?.value

        // Finn det første numeriske tallet i strengen, eller bruk 0
        val temp = tempString?.let { str ->
            // Regex som finner det første tallet
            val match = Regex("""\d+""").find(str)
            match?.value?.toInt()
        } ?: 0

        val vendor = DiskVendor.fromModel(root.modelName)

        return ParsedSataData(
            normalizedWear = normalizedWear,
            rawWear = rawWear,
            temperature = temp,
            vendor = vendor
        )
    }

    fun calculateHealth(parsed: ParsedSataData): Int {
        return when (parsed.vendor) {
            DiskVendor.SAMSUNG,
            DiskVendor.WD_SANDISK -> {
                val normalized = parsed.normalizedWear ?: 100
                100 - normalized
            }

            DiskVendor.INTEL -> {
                val normalized = parsed.normalizedWear ?: 100
                100 - normalized
            }

            DiskVendor.CRUCIAL -> {
                val remaining = parsed.rawWear ?: 100
                100 - remaining
            }

            DiskVendor.GENERIC -> {
                val normalized = parsed.normalizedWear ?: 100
                100 - normalized
            }
        }
    }

    fun toDiskHealth(root: SataRoot, device: String): DiskHealth {
        val parsed = parseCommon(root)
        val percentageUsed = calculateHealth(parsed)

        return DiskHealth(
            deviceName = device,
            modelName = root.modelName,
            serialNumber = root.serialNumber,
            protocol = "SATA",
            isHealthy = root.smartStatus.passed,
            percentageUsed = percentageUsed,
            temperatureCelsius = parsed.temperature
        )
    }
}
