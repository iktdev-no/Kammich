package no.iktdev.kammich.utils

object WifiUtils {
    fun dBmToPercentage(signalDbm: Double): Int {
        if (signalDbm >= -50.0) return 100
        if (signalDbm <= -100.0) return 0
        // Regner ut verdien som Double, konverterer til Int, og tvinger den innenfor 0-100
        return ((signalDbm + 100.0) * 2.0).toInt().coerceIn(0, 100)
    }
    fun dBmToPercentage(signalDbm: Int): Int = dBmToPercentage(signalDbm.toDouble())


    fun parseSecurity(capability: String, authSuites: String): String {
        return when {
            authSuites.contains("PSK") -> "WPA2-Personal"
            authSuites.contains("802.1X") -> "WPA2-Enterprise"
            capability.contains("WEP") -> "WEP"
            capability.contains("ESS") -> "Åpent"
            else -> "Ukjent"
        }
    }
}