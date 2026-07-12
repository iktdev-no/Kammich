package no.iktdev.kammich.system.network

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class WifiRunner {

    private val log = LoggerFactory.getLogger(WifiRunner::class.java)

        /**
         * Piper råtekst inn til 'jc --iw-scan' via standard input.
         * Dette eliminerer behovet for usikre bash-pipes.
         */
        private fun runWithJc(rawInput: String): String {
            return try {
                val process = ProcessBuilder(listOf("jc", "--iw-scan")).start()

                // Skriv rå-output fra iw inn i jc sin stdin
                process.outputStream.bufferedWriter().use { it.write(rawInput) }

                val jsonOutput = process.inputStream.bufferedReader().use { it.readText() }
                val errorOutput = process.errorStream.bufferedReader().use { it.readText() }

                process.waitFor(5, TimeUnit.SECONDS)

                if (process.exitValue() != 0) {
                    log.error("jc feilet under parsing av data. Stderr: ${errorOutput.trim()}")
                    return ""
                }

                jsonOutput
            } catch (e: Exception) {
                log.error("Klarte ikke å kjøre jc-parsing. Er jc installert på systemet?", e)
                ""
            }
        }

    /**
     * Kjører rene kommandoer uten shell-fortolkning, og logger stderr dersom noe feiler.
     */
    fun run(vararg params: String): String {
        val command = params.toList()
        return try {
            val process = ProcessBuilder(command).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val errorOutput = process.errorStream.bufferedReader().use { it.readText() }

            // Siden det er en WiFi-skanning, lar vi den få opptil 12 sekunder på å gjøre jobben ferdig
            val finished = process.waitFor(12, TimeUnit.SECONDS)

            if (!finished) {
                process.destroyForcibly()
                log.error("Kommandoen ${command.joinToString(" ")} timet ut!")
                return ""
            }

            if (process.exitValue() != 0) {
                log.error("Kommandoen feilet med status ${process.exitValue()}. Stderr: ${errorOutput.trim()}")
            }

            output
        } catch (e: Exception) {
            log.error("Kritisk systemfeil under kjøring av pure command: ${command.joinToString(" ")}", e)
            ""
        }
    }
}