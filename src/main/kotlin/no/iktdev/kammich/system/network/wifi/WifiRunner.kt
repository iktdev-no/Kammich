package no.iktdev.kammich.system.network.wifi

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class WifiRunner {

    private val log = LoggerFactory.getLogger(WifiRunner::class.java)

    sealed class CommandResult {
        data class Success(val output: String) : CommandResult()
        data class Failure(val error: String, val exitCode: Int? = null) : CommandResult()

        // Mapper suksess til en ny type, eller returnerer null ved feil/tom input
        fun <T> map(transform: (String) -> T): T? = when (this) {
            is Success -> if (output.isBlank()) null else try { transform(output) } catch (e: Exception) { null }
            is Failure -> null
        }
    }

    /**
     * Kjører kommandoer og returnerer et [CommandResult] fremfor en tom streng.
     */
    fun run(vararg params: String): CommandResult {
        val command = params.toList()
        return try {
            val process = ProcessBuilder(command).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val errorOutput = process.errorStream.bufferedReader().use { it.readText() }

            val finished = process.waitFor(12, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return CommandResult.Failure("Timeout etter 12 sekunder")
            }

            if (process.exitValue() != 0) {
                CommandResult.Failure(errorOutput.trim(), process.exitValue())
            } else {
                CommandResult.Success(output)
            }
        } catch (e: Exception) {
            log.error("Systemfeil under kjøring av: ${command.joinToString(" ")}", e)
            CommandResult.Failure(e.message ?: "Ukjent feil")
        }
    }
}

/**
 * Extension for å pipe CommandResult gjennom jc.
 * Denne kan kjedes direkte etter en run()-kall.
 */
fun WifiRunner.CommandResult.pipeJc(jcParser: String = "--iw-scan"): WifiRunner.CommandResult {
    return when (this) {
        is WifiRunner.CommandResult.Failure -> this
        is WifiRunner.CommandResult.Success -> {
            try {
                val process = ProcessBuilder("jc", jcParser).start()
                process.outputStream.bufferedWriter().use { it.write(this.output) }

                val jsonOutput = process.inputStream.bufferedReader().use { it.readText() }
                val errorOutput = process.errorStream.bufferedReader().use { it.readText() }

                process.waitFor(5, TimeUnit.SECONDS)

                if (process.exitValue() != 0) {
                    WifiRunner.CommandResult.Failure("jc feilet: ${errorOutput.trim()}")
                } else {
                    WifiRunner.CommandResult.Success(jsonOutput)
                }
            } catch (e: Exception) {
                WifiRunner.CommandResult.Failure("Kunne ikke kjøre jc: ${e.message}")
            }
        }
    }
}