package no.iktdev.kammich.database.tables

import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.models.internal.PersistedDevice
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.sql.ResultSet

object DevicesTable : LongIdTable("DEVICES") {
    val deviceName = text("DEVICE_NAME")
    val serialNumber = text("SERIAL_NUMBER").uniqueIndex()
    val deviceType = text("DEVICE_TYPE").nullable()
    val manufacturer = text("MANUFACTURER").nullable()
    val model = text("MODEL").nullable()
    val lastSeen = text("LAST_SEEN")

    fun ResultRow.toPersisted(): PersistedDevice {
        return PersistedDevice(
            id = this[DevicesTable.id].value,
            name = this[DevicesTable.deviceName],
            serialNumber = this[serialNumber],
            manufacturer = this[manufacturer],
            model = this[model],
            deviceType = this[deviceType],
            lastSeen = this[lastSeen]
        )
    }

    fun getDevices(): List<PersistedDevice> {
        return withTransaction {
            DevicesTable.selectAll()
                .mapNotNull { it.toPersisted() }
        }.getOrDefault(emptyList())
    }
}

fun DevicesTable.getDeviceId(serialNumber: String): Long? {
    return withTransaction {
        DevicesTable.select(DevicesTable.id)
            .where { DevicesTable.serialNumber eq serialNumber }
            .singleOrNull()?.get(DevicesTable.id)?.value
    }.getOrNull()
}

fun DevicesTable.getDeviceSerialNumber(pk: Long): String? {
    return withTransaction {
        DevicesTable.select(DevicesTable.serialNumber)
            .where { DevicesTable.id eq pk }
            .singleOrNull()?.get(DevicesTable.serialNumber)
    }.getOrNull()
}