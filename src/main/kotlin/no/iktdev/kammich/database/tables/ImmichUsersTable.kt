package no.iktdev.kammich.database.tables

import no.iktdev.kammich.database.models.PersistedImmichUser
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object ImmichUsersTable: IntIdTable("IMMICH_USERS") {
    val userId = varchar("USER_ID", 36).uniqueIndex()
    val name = text("NAME")
    val email = text("EMAIL")
    val createdAt = text("CREATED_AT")
    val isActive = bool("IS_ACTIVE")
    val data = text("DATA")

    fun ResultRow.toPersistedImmichUser(): PersistedImmichUser {
        return PersistedImmichUser(
            userId = this[ImmichUsersTable.userId],
            name = this[ImmichUsersTable.name],
            email = this[ImmichUsersTable.email],
            createdAt = this[ImmichUsersTable.createdAt],
            isActive = this[ImmichUsersTable.isActive],
            data = this[ImmichUsersTable.data],
        )
    }
}