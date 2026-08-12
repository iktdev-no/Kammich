package no.iktdev.kammich.database.tables

import no.iktdev.kammich.database.models.PersistedDeviceOwner
import no.iktdev.kammich.database.models.PersistedJobOwner
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.*

object DeviceOwnerTable : LongIdTable("DEVICE_OWNER") {
    val deviceSN = text("SERIAL_NUMBER")
    val immichUserId = varchar("IMMICH_USER_ID", 36)

    init {
        // Legger til UNIQUE-constraint på kombinasjonen av DEVICE_ID og IMMICH_USER_ID
        uniqueIndex(deviceSN, immichUserId)
    }

    fun ResultRow.toPersistedDeviceOwner(): PersistedDeviceOwner = PersistedDeviceOwner(
        deviceSerialNumber = this[deviceSN],
        immichUserId = this[immichUserId].let { UUID.fromString(it) },
    )

}

object ImportJobOwnerTable : LongIdTable("IMPORT_JOB_OWNER") {
    val importJob = varchar("IMPORT_JOB", 36).uniqueIndex()
    val immichUserId = varchar("IMMICH_USER_ID", 36)

    fun getWhere(predicate: () -> Op<Boolean>): List<PersistedJobOwner> {
        return ImportJobOwnerTable.selectAll()
            .where(predicate) // Kan nå også skrives direkte som .where { ... }
            .orderBy(ImportJobOwnerTable.id, SortOrder.DESC)
            .map { it.toPersistedJobOwner() }
    }

    fun ResultRow.toPersistedJobOwner(): PersistedJobOwner {
        return PersistedJobOwner(
            jobId = this[importJob].let { UUID.fromString(it) },
            immichUserId = this[immichUserId].let { UUID.fromString(it) },
        )
    }
}