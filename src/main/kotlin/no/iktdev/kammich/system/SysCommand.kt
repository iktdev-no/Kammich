package no.iktdev.kammich.system

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class SysCommand {
    private val log = LoggerFactory.getLogger(javaClass)

    sealed class Result() {
        data class Success(val output: String): Result()
        data class Failure(val output: String? = null, val errOutput: String? = null, val exitCode: Int? = null): Result()

        fun isSuccess() = this is Success
        fun isFailure() = this is Failure

        fun <T> fold(
            onSuccess: ((output: String) -> T)? = null,
            onFailure: ((output: String?, errOutput: String?, exitCode: Int?) -> T)? = null
        ): T? {
            return when (this) {
                is Success -> onSuccess?.invoke(output)
                is Failure -> onFailure?.invoke(output, errOutput, exitCode)
            }
        }

        fun <T> getOrDefault(default: T, transform: (String) -> T): T {
            return when (this) {
                is Success -> transform(output)
                is Failure -> default
            }
        }

        fun getOrNull(): String? {
            return when (this) {
                is Success -> output
                is Failure -> null
            }
        }
    }

    fun nonSudo(vararg params: String): Result = nonSudo(params.toList())
    fun nonSudo(command: List<String>): Result {
        return runCommand(command)
    }

    fun sudo(vararg params: String): Result = sudo(params.toList())
    fun sudo(command: List<String>): Result {
       return runCommand(listOf("sudo") + command)
    }

    private fun runCommand(command: List<String>): Result {
        return try {
            val process = ProcessBuilder(command).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val errorOutput = process.errorStream.bufferedReader().use { it.readText() }

            val finished = process.waitFor(12, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return Result.Failure("Timeout etter 12 sekunder")
            }

            if (process.exitValue() != 0) {
                log.error("Could not run command successfully (${process.exitValue()}): ${command.joinToString(" ")}\nOutput:\n${output}\nError:\n${errorOutput}")
                Result.Failure(output, errorOutput.trim(), process.exitValue())
            } else {
                Result.Success(output)
            }
        } catch (e: Exception) {
            log.error("Systemfeil under kjøring av: ${command.joinToString(" ")}", e)
            Result.Failure(e.message ?: "Ukjent feil")
        }
    }


}