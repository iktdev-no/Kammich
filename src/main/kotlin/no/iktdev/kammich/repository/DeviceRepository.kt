package no.iktdev.kammich.repository

import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.models.shared.storage.removable.Device
import no.iktdev.kammich.models.shared.storage.removable.DeviceInfo
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.upsert
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class DeviceRepository {
    private val log = LoggerFactory.getLogger(DeviceRepository::class.java)

    fun store(device: Device, info: DeviceInfo?): Boolean {
        log.info("Storing/Updating ${device.id}")

        return withTransaction {
            DevicesTable.upsert(DevicesTable.serialNumber) { // Angi kolonnen som er unik
                it[this.serialNumber] = device.id
                it[this.deviceName] = device.name
                it[this.model] = device.model
                it[this.lastSeen] = ZonedDateTime.now().toString()

                // Bare oppdater hvis verdiene ikke er null,
                // eller sett de hvis det er ny rad
                info?.let { i ->
                    it[this.friendlyName] = i.friendlyName
                    it[this.manufacturer] = i.manufacturer
                }
            }
        }.isSuccess
    }
}