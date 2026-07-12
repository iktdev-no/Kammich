package no.iktdev.kammich

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import jakarta.annotation.PostConstruct
import no.iktdev.kammich.gphoto2.GPhoto2
import no.iktdev.kammich.gphoto2.IGPhoto2
import org.flywaydb.core.Flyway
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import javax.sql.DataSource

@Configuration
class CorsConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")
            .allowedMethods("*")
            .allowCredentials(false)
    }
}

@Configuration
class ApplicationConfiguration {

    @Bean
    fun gphoto2(): IGPhoto2 {
        return GPhoto2()
    }

    @Bean fun gson(): Gson {
        return GsonBuilder().setPrettyPrinting().create()
    }
}
