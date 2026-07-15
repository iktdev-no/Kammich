package no.iktdev.kammich.system.network.wifi.parser

class WifiPhyInfoParser {

    data class WifiCapability(
        val supportsAP: Boolean,
        val isConcurrent: Boolean,
        val sameChannelConstraint: Boolean
    )

    fun parse(output: String): WifiCapability {
        var supportsAP = false
        var isConcurrent = false
        var sameChannelConstraint = false

        var currentSection = Section.UNKNOWN

        output.lineSequence().forEach { line ->
            val trimmed = line.trim()

            // 1. Identifiser seksjon (uavhengig av whitespace)
            when {
                trimmed.startsWith("Supported interface modes:") -> currentSection = Section.MODES
                trimmed.startsWith("valid interface combinations:") -> currentSection = Section.COMBINATIONS
                trimmed.isNotEmpty() && !line.startsWith(" ") && !line.startsWith("\t") -> currentSection = Section.UNKNOWN
            }

            // 2. Analyser innhold basert på seksjon
            when (currentSection) {
                Section.MODES -> {
                    if (trimmed.contains("* AP") || trimmed.endsWith("AP")) {
                        supportsAP = true
                    }
                }
                Section.COMBINATIONS -> {
                    // Vi ser etter en linje som inneholder BÅDE managed og AP
                    if (trimmed.contains("managed") && trimmed.contains("AP")) {
                        isConcurrent = true

                        // Sjekk for kanal-begrensning i samme kombinasjon
                        if (trimmed.contains("channels <= 1") ||
                            output.substringAfter(line).contains("channels <= 1")) {
                            sameChannelConstraint = true
                        }
                    }
                }
                else -> {}
            }
        }

        return WifiCapability(supportsAP, isConcurrent, sameChannelConstraint)
    }

    fun getWifiInfoFromInterface(output: String): Pair<Int, Int>? {
        // Vi leter etter linjen: "channel 11 (2462 MHz)"
        val channelLine = output.lines().find { it.contains("channel") && it.contains("MHz") } ?: return null

        // Regex for å hente ut: kanal og frekvens (MHz)
        // Eksempel: channel (\d+) \((\d+) MHz\)
        val regex = Regex("""channel\s+(\d+)\s+\((\d+)\s+MHz\)""")
        val match = regex.find(channelLine)

        return match?.let {
            val channel = it.groupValues[1].toInt()
            val freq = it.groupValues[2].toInt()
            Pair(channel, freq)
        }
    }


    private enum class Section {
        UNKNOWN, MODES, COMBINATIONS
    }
}