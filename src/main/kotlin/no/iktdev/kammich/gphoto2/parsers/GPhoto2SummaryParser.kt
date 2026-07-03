package no.iktdev.kammich.gphoto2.parsers

import no.iktdev.kammich.gphoto2.model.GPhoto2Summary
import no.iktdev.kammich.gphoto2.model.GPhoto2StorageDevice

class GPhoto2SummaryParser : GPhoto2Parser<GPhoto2Summary> {

    override fun parse(input: String): GPhoto2Summary {
        val lines = input.lines()

        return GPhoto2Summary(
            manufacturer = getValue(lines, "Manufacturer"),
            model = getValue(lines, "Model"),
            serialNumber = getValue(lines, "Serial Number"),
            // Regex henter første tall etter "value:"
            batteryLevel = Regex("value:\\s*(\\d+)").find(input)?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            // Henter ut mellom klammeparenteser: ('bskjon')
            friendlyDeviceName = Regex("Friendly Device Name.*?:\\s*(.*?)\\s*\\(", RegexOption.IGNORE_CASE)
                .find(input)
                ?.groupValues?.get(1)
                ?.trim(),
            storageDevices = parseStorage(input)
        )
    }

    private fun parseStorage(input: String): List<GPhoto2StorageDevice> {
        // Regex finner alle blokker som starter med store_
        val storageBlocks = Regex("store_([0-9a-fA-F]+):").findAll(input)

        return storageBlocks.map { match ->
            val id = match.groupValues[1]
            // Vi henter ut hele tekstblokken tilhørende denne storage-IDen
            val block = input.substringAfter("store_$id:").substringBefore("store_")

            GPhoto2StorageDevice(
                id = "store_$id",
                description = getBlockValue(block, "StorageDescription"),
                capacityBytes = extractBytes(block, "Maximum Capability"),
                freeSpaceBytes = extractBytes(block, "Free Space")
            )
        }.toList()
    }

    private fun getValue(lines: List<String>, key: String): String? {
        // Regex: Start på linje, nøkkelen, valgfri whitespace, kolon, så resten
        val regex = Regex("^$key\\s*:", RegexOption.IGNORE_CASE)

        return lines.find { regex.containsMatchIn(it) }
            ?.substringAfter(":")
            ?.trim()
            ?.takeIf { it.isNotEmpty() } // Returner null hvis strengen ble tom
    }

    private fun getBlockValue(block: String, key: String) =
        block.lines().find { it.contains(key, true) }?.substringAfter(":")?.substringBefore("bytes")?.trim() ?: ""

    private fun extractBytes(block: String, key: String): Long {
        val line = block.lines().find { it.contains(key, true) } ?: return 0L
        return Regex("(\\d+)").find(line)?.value?.toLongOrNull() ?: 0L
    }
}