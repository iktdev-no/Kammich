package no.iktdev.kammich.storage.internal

import com.google.gson.Gson
import com.google.gson.JsonParser
import no.iktdev.kammich.models.storage.DiskHealth
import no.iktdev.kammich.models.storage.NvmeRoot
import no.iktdev.kammich.models.storage.SataRoot
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SmartCtlService {

    private val log = LoggerFactory.getLogger(SmartCtlService::class.java)


    fun getSMART(path: String): Result<DiskHealth> = runCatching {
        val jsonString = getRawJson(path)
        val result = mapToDiskHealth(jsonString, path)
        result
    }

    fun getRawJson(device: String): String {
        // Her kjører vi den faktiske kommandoen
        return ProcessBuilder("sudo", "smartctl", "--json", "-x", device)
            .start()
            .inputStream
            .bufferedReader()
            .use { it.readText() }
    }

    private fun mapToDiskHealth(jsonString: String, device: String): DiskHealth {
        val jsonObject = JsonParser.parseString(jsonString).asJsonObject
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