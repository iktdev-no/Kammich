package no.iktdev.kammich.storage.internal

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import no.iktdev.kammich.models.shared.storage.DiskHealth
import no.iktdev.kammich.models.shared.storage.NvmeRoot
import no.iktdev.kammich.models.shared.storage.SataRoot
import no.iktdev.kammich.system.SysCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SmartCtlService(
    private val exec: SysCommand
) {

    private val log = LoggerFactory.getLogger(SmartCtlService::class.java)


    fun getSMART(path: String): Result<DiskHealth> = runCatching {
        val jsonString = getRawJson(path) ?: throw RuntimeException("Failed to get JSON from $path")
        val result = mapToDiskHealth(jsonString, path)
        result
    }

    fun getRawJson(device: String): String? {
       return exec.sudo("smartctl", "--json", "-x", device).getOrNull()
        // Her kjører vi den faktiske kommandoen
    }

    private fun mapToDiskHealth(jsonString: String, device: String): DiskHealth {
        val jsonObject = try {
            JsonParser.parseString(jsonString).asJsonObject
        } catch (e: Exception) {
            log.error("Error parsing json object: $jsonString")
            throw e
        }
        val protocol = jsonObject.getAsJsonObject("device").get("protocol").asString.lowercase()
        val gson = Gson()

        return when (protocol) {
            "nvme" -> {
                val root = gson.fromJson(jsonString, NvmeRoot::class.java)
                DiskHealth(
                    device, root.modelName, root.serialNumber, "NVMe",
                    root.smartStatus.passed, root.log.pUsed, root.log.temp
                )
            }

            "sata", "ata" -> {
                val root = gson.fromJson(jsonString, SataRoot::class.java)
                DiskVendorParsing.toDiskHealth(root, device)
            }

            else -> throw IllegalArgumentException("Ukjent protokoll: $protocol")
        }
    }


}