package no.iktdev.kammich.utils

object DiskUtils {
    // Liste over produsenter vi vil "trekke ut"
    private val knownManufacturers = listOf(
        "sk hynix", "solidigm", "samsung", "western digital", "wdc",
        "sandisk", "seagate", "crucial", "micron", "kioxia",
        "toshiba", "kingston", "adata", "lexar", "pny",
        "corsair", "teamgroup", "intel", "hgst", "msi",
        "gigabyte", "asus"
    )

    fun parseDeviceVendorAndModel(model: String?): Pair<String, String>? {
        if (model.isNullOrBlank()) return null

        val modelLower = model.lowercase()

        // Finn den produsenten som matcher starten av modellnavnet
        val manufacturer = knownManufacturers.find { modelLower.startsWith(it) }

        return if (manufacturer != null) {
            val cleanModel = model.substring(manufacturer.length).trim()
            val prettyManufacturer = manufacturer.split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

            prettyManufacturer to cleanModel
        } else {
            null
        }
    }
}