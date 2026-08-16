package no.iktdev.kammich.system.network.v1.wifi.parser

import no.iktdev.kammich.models.shared.network.WirelessNetworkInterfaceCapability

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

    fun parseCapabilities(output: String): Set<WirelessNetworkInterfaceCapability> {
        val capabilities = mutableSetOf<WirelessNetworkInterfaceCapability>()

        // Anta alltid STA støtte hvis det er et trådløst kort (Managed mode er standard)
        capabilities.add(WirelessNetworkInterfaceCapability.STA)

        var currentSection: Section = Section.UNKNOWN
        val lines = output.lineSequence().map { it.trim() }.toList()

        lines.forEach { line ->
            // 1. Seksjons-deteksjon
            when {
                line.startsWith("Supported interface modes:") -> currentSection = Section.MODES
                line.startsWith("valid interface combinations:") -> currentSection = Section.COMBINATIONS
                line.isNotEmpty() && !line.startsWith("*") && !line.startsWith("(") -> currentSection = Section.UNKNOWN
            }

            // 2. Kapasitets-deteksjon
            when (currentSection) {
                Section.MODES -> {
                    if (line.contains("* AP")) {
                        capabilities.add(WirelessNetworkInterfaceCapability.AP)
                    }
                }
                Section.COMBINATIONS -> {
                    // Sjekk etter konkurrent-støtte
                    if (line.contains("managed") && line.contains("AP")) {
                        capabilities.add(WirelessNetworkInterfaceCapability.Concurrent)

                        // Sjekk etter kanal-begrensning
                        if (line.contains("channels <= 1")) {
                            capabilities.add(WirelessNetworkInterfaceCapability.Concurrent_Restricted_Same_Channel)
                        }
                    }
                }
                Section.UNKNOWN -> {}
            }
        }

        return capabilities
    }

    private enum class Section {
        UNKNOWN, MODES, COMBINATIONS
    }
}