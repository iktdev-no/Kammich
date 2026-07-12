package no.iktdev.kammich

import com.google.gson.GsonBuilder
import no.iktdev.kammich.models.shared.config.KammichConfig
import org.springframework.stereotype.Service
import java.io.File

@Service
class ConfigService {
    private val configFile = File("./kammich.json") // Eller en mer robust sti
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var _config: KammichConfig = loadConfig()
    fun getConfig(): KammichConfig = _config

    private fun loadConfig(): KammichConfig {
        return if (configFile.exists()) {
            gson.fromJson(configFile.readText(), KammichConfig::class.java)
        } else {
            val defaultConfig = KammichConfig(
                "/var/lib/kammich/storage/media",
            )
            saveConfig(defaultConfig) // Lagre standard om fila ikke finnes
            defaultConfig
        }
    }

    @Synchronized
    fun saveConfig(newConfig: KammichConfig) {
        configFile.parentFile.mkdirs()
        configFile.writeText(gson.toJson(newConfig))
        _config = newConfig
    }
}