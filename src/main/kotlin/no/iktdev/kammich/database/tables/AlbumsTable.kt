package no.iktdev.kammich.database.tables

import no.iktdev.kammich.models.internal.PersistedAlbum
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

object AlbumsTable : LongIdTable("ALBUMS") {
    val title = text("TITLE")
    val immichUserId = varchar("IMMICH_USER_ID", 36)
    val description = text("DESCRIPTION").nullable()
    val startDate = text("START_DATE").nullable()
    val endDate = text("END_DATE").nullable()
    val use = bool("USE").default(true)
    val immichAlbumId = varchar("IMMICH_ALBUM_ID", 36)
    val createdAt = text("CREATED_AT")

    init {
        foreignKey(
            immichUserId to ImmichUsersTable.userId,
            onDelete = ReferenceOption.CASCADE
        )
    }


    fun getWhere(predicate: () -> Op<Boolean>): List<PersistedAlbum> {
        return AlbumsTable.selectAll()
            .where(predicate) // Kan nå også skrives direkte som .where { ... }
            .orderBy(AlbumsTable.id, SortOrder.DESC)
            .map { it.toPersistedAlbum()}
    }

    fun ResultRow.toPersistedAlbum() = PersistedAlbum(
        id = this[id].value,
        name = this[title],
        description = this[description],
        immichUserId = this[immichUserId].let { x -> UUID.fromString(x) },
        immichAlbumId = this[immichAlbumId].let { x -> UUID.fromString(x) },
        createdAt = parseInstantOrDate(this[createdAt]) ?: Instant.now(),
        startDate = parseInstantOrDate(this[startDate]),
        endDate = parseInstantOrDate(this[endDate]),
        use = this[use]
    )

    private fun parseInstantOrDate(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return try {
            // Hvis strengen allerede har tidsstempel/Z (f.eks. ISO)
            Instant.parse(value)
        } catch (e: Exception) {
            try {
                // Takler "YYYY-MM-DD" fra MUI datepicker ved å bruke systemets tidssone
                LocalDate.parse(value)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
            } catch (e2: Exception) {
                null
            }
        }
    }
}