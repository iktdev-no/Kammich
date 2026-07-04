package no.iktdev.kammich

import no.iktdev.kammich.gphoto2.GPhoto2
import no.iktdev.kammich.gphoto2.IGPhoto2
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

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
}