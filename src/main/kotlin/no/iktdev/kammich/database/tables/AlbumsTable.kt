package no.iktdev.kammich.database.tables

import no.iktdev.kammich.models.internal.PersistedAlbum
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant
import java.util.*

object AlbumsTable : LongIdTable("ALBUMS") {
    val title = text("TITLE")
    val immichUserId = varchar("IMMICH_USER_ID", 36)
    val description = text("DESCRIPTION").nullable()
    val startDate = text("START_DATE").nullable()
    val endDate = text("END_DATE").nullable()
    val use = bool("USE").default(true)
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
            .map {
                PersistedAlbum(
                    id = it[id].value,
                    name = it[title],
                    description = it[description],
                    immichUserId = it[immichUserId].let { x -> UUID.fromString(x) },
                    createdAt = it[createdAt].let { x -> Instant.parse(x) },
                    startDate = it[startDate]?.let { x -> Instant.parse(x) },
                    endDate = it[endDate]?.let { x -> Instant.parse(x) }
                )
            }
    }
}