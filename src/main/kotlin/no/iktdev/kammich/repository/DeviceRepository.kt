package no.iktdev.kammich.repository

import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.models.shared.device.RemovableDevice
import org.jetbrains.exposed.v1.jdbc.upsert
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class DeviceRepository {
    private val log = LoggerFactory.getLogger(DeviceRepository::class.java)

    fun store(device: RemovableDevice): Boolean {
        log.info("Storing/Updating ${device.id}")

        return withTransaction {
            DevicesTable.upsert(DevicesTable.serialNumber) { // Angi kolonnen som er unik
                it[this.serialNumber] = device.id
                it[this.deviceName] = device.name
                it[this.model] = device.model
                it[this.lastSeen] = ZonedDateTime.now().toString()
                it[this.manufacturer] = device.manufacturer
            }
        }.isSuccess
    }
}