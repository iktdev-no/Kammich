package no.iktdev.kammich.gphoto2.parsers

import no.iktdev.kammich.gphoto2.model.GPhoto2DiscoveredDevice

class GPhoto2ConnectedDeviceParser : GPhoto2Parser<List<GPhoto2DiscoveredDevice>> {

    // Regex for å validere at porten starter med et kjent prefiks (usb, ptp, serial, etc.)
    private val validPortRegex = Regex("^(usb|ptp|serial):.*", RegexOption.IGNORE_CASE)

    override fun parse(input: String): List<GPhoto2DiscoveredDevice> {
        return input.lines()
            .drop(2) // Hopper over header og skillelinje
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s{2,}"))
                    .filter(String::isNotBlank)

                if (parts.size == 2) {
                    val model = parts[0].trim()
                    val port = parts[1].trim()

                    // Validerer at port-strengen matcher forventet format
                    if (validPortRegex.matches(port)) {
                        GPhoto2DiscoveredDevice(model, port)
                    } else {
                        null // Linje som ikke har gyldig port-format forkastes
                    }
                } else null
            }
    }
}