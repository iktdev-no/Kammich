package no.iktdev.kammich.system.network.wifi.parser

abstract class NmcliHelper {
    private val bssidRegex = Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}")

    fun parseLine(line: String): List<String> {
        // Fjern escapes og splitt
        val cleanLine = line.replace("\\", "")
        return cleanLine.split(":")
    }

    fun findBssid(parts: List<String>): String {
        return parts.find { it.matches(bssidRegex) } ?: "00:00:00:00:00:00"
    }

    fun findSignal(parts: List<String>): Int {
        // Ser etter feltet som er et tall mellom 0 og 100
        return parts.find { it.toIntOrNull() in 0..100 }?.toIntOrNull() ?: 0
    }
}