package no.iktdev.kammich.storage.internal

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import no.iktdev.kammich.models.shared.storage.DiskHealth
import no.iktdev.kammich.models.shared.storage.DiskVariant
import no.iktdev.kammich.models.shared.storage.NvmeRoot
import no.iktdev.kammich.models.shared.storage.SataRoot
import no.iktdev.kammich.system.SysCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SmartCtlService(
    private val exec: SysCommand
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val gson = Gson()

    fun getSMART(path: String): Result<DiskHealth> {
        val commandResult = exec.sudo(
            "smartctl",
            "--json",
            "-x",
            path
        )

        val output = commandResult.fold(
            onSuccess = { output ->
                output
            },
            onFailure = { output, errorOutput, exitCode ->
                if (output.isNullOrBlank()) {
                    log.error(
                        "smartctl failed for {}. Exit code: {}, error: {}",
                        path,
                        exitCode,
                        errorOutput
                    )
                }

                output
            }
        )

        if (output.isNullOrBlank()) {
            return Result.failure(
                IllegalStateException(
                    "smartctl returned no output for $path"
                )
            )
        }

        val jsonObject = try {
            JsonParser
                .parseString(output)
                .takeIf { it.isJsonObject }
                ?.asJsonObject
                ?: return Result.failure(
                    IllegalStateException(
                        "smartctl returned JSON that is not an object for $path"
                    )
                )
        } catch (e: JsonParseException) {
            log.error(
                "smartctl returned invalid JSON for {}",
                path,
                e
            )

            return Result.failure(e)
        }

        return try {
            Result.success(
                mapToDiskHealth(
                    jsonObject = jsonObject,
                    jsonString = output,
                    device = path
                )
            )
        } catch (e: Exception) {
            log.error(
                "Failed to map smartctl response for {}",
                path,
                e
            )

            Result.failure(e)
        }
    }

    private fun mapToDiskHealth(
        jsonObject: com.google.gson.JsonObject,
        jsonString: String,
        device: String
    ): DiskHealth {
        val protocol = jsonObject
            .getAsJsonObject("device")
            .get("protocol")
            .asString
            .lowercase()

        val diskVariant = getDiskVariant(
            jsonObject = jsonObject,
            protocol = protocol
        )

        return when (protocol) {
            "nvme" -> {
                val root = gson.fromJson(
                    jsonString,
                    NvmeRoot::class.java
                )

                DiskHealth(
                    deviceName = device,
                    modelName = root.modelName,
                    serialNumber = root.serialNumber,
                    protocol = "NVMe",
                    isHealthy = root.smartStatus.passed,
                    percentageUsed = root.log.pUsed,
                    temperatureCelsius = root.log.temp,
                    diskVariant = diskVariant
                )
            }

            "sata", "ata" -> {
                val root = gson.fromJson(
                    jsonString,
                    SataRoot::class.java
                )

                DiskVendorParsing.toDiskHealth(
                    root,
                    device,
                    diskVariant
                )
            }

            else -> {
                throw IllegalArgumentException(
                    "Ukjent protokoll: $protocol"
                )
            }
        }
    }

    private fun getDiskVariant(
        jsonObject: com.google.gson.JsonObject,
        protocol: String
    ): DiskVariant {
        if (protocol == "nvme") {
            return DiskVariant.SSD
        }

        val rotationRate = jsonObject
            .get("rotation_rate")
            ?.takeIf { it.isJsonPrimitive }
            ?.asInt

        return when {
            rotationRate == 0 -> DiskVariant.SSD
            rotationRate != null && rotationRate > 0 -> DiskVariant.HDD
            else -> {
                throw IllegalArgumentException(
                    "Kunne ikke bestemme diskvariant. " +
                            "rotation_rate mangler for $protocol"
                )
            }
        }
    }
}