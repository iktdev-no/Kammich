package no.iktdev.kammich.system

import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class SystemCommandService {

    /**
     * Kjører kommandoer med 'sudo' foran.
     * @param command Kommandoen delt opp, f.eks. "systemctl", "restart", "hostapd"
     */
    fun runNetworkCommand(vararg command: String): String {
        // Vi tvinger alltid 'sudo' foran for å bruke reglene i sudoers-filen
        val fullCommand = listOf("sudo") + command.toList()
        
        val process = ProcessBuilder(fullCommand)
            .redirectErrorStream(true) // Slår sammen stdout og stderr
            .start()

        // Vent maks 10 sekunder på svar
        val finished = process.waitFor(10, TimeUnit.SECONDS)
        
        if (!finished) {
            process.destroy()
            throw RuntimeException("Kommandoen tidsavbrøt: ${fullCommand.joinToString(" ")}")
        }

        val output = process.inputStream.bufferedReader().readText()
        
        if (process.exitValue() != 0) {
            throw RuntimeException("Kommando feilet (exit ${process.exitValue()}): $output")
        }
        
        return output.trim()
    }

}