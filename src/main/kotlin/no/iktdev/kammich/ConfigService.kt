package no.iktdev.kammich

import com.google.gson.GsonBuilder
import no.iktdev.kammich.models.internal.config.RuntimeKammichConfig
import no.iktdev.kammich.models.internal.config.StoredKammichConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class ConfigService {
    private val log = LoggerFactory.getLogger(ConfigService::class.java)


    private val configFile = File("./kammich.json") // Eller en mer robust sti
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var _config: RuntimeKammichConfig = loadConfig()
    fun getConfig(): RuntimeKammichConfig = _config

    private fun loadConfig(): RuntimeKammichConfig {
        return if (configFile.exists()) {
            val stored = gson.fromJson(configFile.readText(), StoredKammichConfig::class.java)
            RuntimeKammichConfig.fromStored(stored)
        } else {
            val defaultConfig = RuntimeKammichConfig()
            saveConfig(defaultConfig) // Lagre standard om fila ikke finnes
            defaultConfig
        }
    }

    @Synchronized
    fun updateConfig(transform: (RuntimeKammichConfig) -> RuntimeKammichConfig) {
        val updatedConfig = transform(_config)
        saveConfig(updatedConfig)
    }

    @Synchronized
    fun saveConfig(newConfig: RuntimeKammichConfig) {
        configFile.parentFile.mkdirs()
        val nc = gson.toJson(newConfig)
        log.debug("Saving config: $nc")
        configFile.writeText(nc)
        _config = newConfig
    }
}