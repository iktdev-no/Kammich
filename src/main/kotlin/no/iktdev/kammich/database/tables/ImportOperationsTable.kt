package no.iktdev.kammich.database.tables

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object ImportOperationsTable : IntIdTable("IMPORT_OPERATIONS") {
    val deviceId = reference("DEVICE_ID", DevicesTable)
    val newFilesCount = integer("NEW_FILES_COUNT")
    val ranAt = text("RAN_AT")
}