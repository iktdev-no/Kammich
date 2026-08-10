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
        // Finn startposisjonen til Storage Devices Summary-seksjonen for å ha et tak
        val storageSection = input.substringAfter("Storage Devices Summary:", "").substringBefore("Device Property Summary")

        // Finner alle enheter som starter med store_
        val storageBlocks = Regex("store_([0-9a-fA-F]+):").findAll(storageSection)

        return storageBlocks.map { match ->
            val id = match.groupValues[1]
            val blockStart = match.range.first

            // Finn hvor neste enhet starter, eller bruk slutten av seksjonen
            val nextMatch = Regex("store_[0-9a-fA-F]+:").find(storageSection, match.range.last + 1)
            val block = if (nextMatch != null) {
                storageSection.substring(blockStart, nextMatch.range.first)
            } else {
                storageSection.substring(blockStart)
            }

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