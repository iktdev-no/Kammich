package no.iktdev.kammich

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import jakarta.annotation.PostConstruct
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import javax.sql.DataSource


@Component("ExposedInit")
class ExposedInitializer(
    private val dataSource: DataSource,
    private val environment: Environment,
) : InitializingBean {

    private val log = LoggerFactory.getLogger(ExposedInitializer::class.java)


    override fun afterPropertiesSet() {
        log.info("Connecting to database")
        Database.connect(dataSource)
    }
}

@Configuration
class DatabaseConfiguration {

    @Bean
    fun dataSource(): DataSource {
        val config = HikariConfig().apply {
            // Bruker SQLite-driver og peker direkte på filen din
            jdbcUrl = "jdbc:sqlite:./kammich.db"
            driverClassName = "org.sqlite.JDBC"
            maximumPoolSize = 1
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_SERIALIZABLE" // Bedre for SQLite
            validate()
        }
        return HikariDataSource(config)
    }
}

@Configuration
@EnableConfigurationProperties(FlywayProperties::class)
@ConditionalOnProperty(name = ["spring.flyway.enabled"], havingValue = "true", matchIfMissing = true)
class FlywayAutoConfig(
    private val dataSource: DataSource,
    private val props: FlywayProperties
) {

    private val log = LoggerFactory.getLogger(FlywayAutoConfig::class.java)

    @PostConstruct
    fun migrate() {
        val locations = props.locations.ifEmpty { listOf("classpath:flyway") }

        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(*locations.toTypedArray())
            .baselineOnMigrate(true)
            .load()

        val pending = flyway.info().pending()
        var migrationsToApply = true
        when {
            pending.isEmpty() -> {
                log.info("ℹ️ Flyway is up to date. No migrations to apply.")
                migrationsToApply = false
            }

            else -> {
                log.info("📦 Pending migrations: ${pending.joinToString { it.script }}")
            }
        }

        val result = flyway.migrate()

        if (result.migrationsExecuted > 0) {
            log.info("✅ Applied ${result.migrationsExecuted} migration(s).")
        } else {
            if (migrationsToApply) {
                log.info("ℹ️ No migrations were applied.")
            }
        }
    }

}

@ConfigurationProperties(prefix = "spring.flyway")
data class FlywayProperties(
    var locations: List<String> = emptyList()
)