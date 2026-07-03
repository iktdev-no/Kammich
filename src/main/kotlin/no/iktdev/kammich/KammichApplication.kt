package no.iktdev.kammich

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class KammichApplication

fun main(args: Array<String>) {
	runApplication<KammichApplication>(*args)
}


