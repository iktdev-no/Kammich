package no.iktdev.kammich.immich

import com.google.gson.Gson
import no.iktdev.kammich.database.models.PersistedImmichAuthentication
import no.iktdev.kammich.database.tables.ImmichAuthenticationTable
import no.iktdev.kammich.database.tables.ImmichAuthenticationTable.toPersistedApiKey
import no.iktdev.kammich.database.tables.ImmichUsersTable
import no.iktdev.kammich.database.tables.ImmichUsersTable.toPersistedImmichUser
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.models.shared.immich.ImmichServerAccess
import no.iktdev.kammich.models.shared.immich.ImmichUserAccesses
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPostResponse
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPostResponseDto
import no.iktdev.kammich.models.shared.immich.api.ImmichUserMe
import no.iktdev.kammich.util.gson
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository


@Repository
class ImmichRepository {

    fun findActiveApiKeyForUserAndServer(userIdStr: String, serverUrl: String): PersistedImmichAuthentication? {
        return withTransaction {
            ImmichAuthenticationTable.selectAll()
                        .where {
                            (ImmichAuthenticationTable.userId eq userIdStr) and
                                    (ImmichAuthenticationTable.serverUrl eq serverUrl) and
                                    (ImmichAuthenticationTable.isActive eq true)
                        }
                        .singleOrNull()?.toPersistedApiKey()
        }.getOrNull()
    }

    fun findApiKeysFor(userIdStr: String, serverUrl: String): List<PersistedImmichAuthentication> {
        return withTransaction {
            ImmichAuthenticationTable.selectAll()
                .where {
                    (ImmichAuthenticationTable.userId eq userIdStr) and
                            (ImmichAuthenticationTable.serverUrl eq serverUrl) and
                            (ImmichAuthenticationTable.isActive eq true)
                }
                .map { it -> it.toPersistedApiKey() }
        }.getOrDefault(emptyList())
    }

    fun getActiveUser(): ImmichUserMe? {
        val pusr =  withTransaction {
            ImmichUsersTable.selectAll()
                .where { ImmichUsersTable.isActive eq true }
                .map { it.toPersistedImmichUser() }
                .singleOrNull()
        }.getOrNull() ?: return null
        return gson.fromJson(pusr.data, ImmichUserMe::class.java)
    }

    fun setActiveUserAndServer(userIdStr: String, apiKeyId: String, serverUrl: String, secret: String, apiKeyDto: ImmichApiKeyPostResponse, me: ImmichUserMe) {
        withTransaction {
            // Deaktiver alle brukere
            ImmichUsersTable.update({ ImmichUsersTable.isActive eq true }) { it[isActive] = false }

            // Opprett/oppdater bruker
            val existingUser = ImmichUsersTable.selectAll().where { ImmichUsersTable.userId eq userIdStr }.singleOrNull()
            if (existingUser == null) {
                ImmichUsersTable.insert {
                    it[ImmichUsersTable.userId] = userIdStr
                    it[name] = me.name
                    it[email] = me.email
                    it[createdAt] = me.createdAt.toString()
                    it[isActive] = true
                    it[data] = gson.toJson(me)
                }
            } else {
                ImmichUsersTable.update({ ImmichUsersTable.userId eq userIdStr }) {
                    it[name] = me.name
                    it[email] = me.email
                    it[isActive] = true
                    it[data] = gson.toJson(me)
                }
            }

            // Deaktiver gamle nøkler for brukeren, sett denne til aktiv
            ImmichAuthenticationTable.update({ ImmichAuthenticationTable.userId eq userIdStr }) { it[isActive] = false }
            ImmichAuthenticationTable.insert {
                it[ImmichAuthenticationTable.userId] = userIdStr
                it[ImmichAuthenticationTable.apiKeyId] = apiKeyId
                it[ImmichAuthenticationTable.serverUrl] = serverUrl
                it[apiKey] = secret
                it[createdAt] = apiKeyDto.apiKey.createdAt.toString()
                it[isActive] = true
                it[data] = gson.toJson(apiKeyDto.apiKey)
            }
        }
    }

    fun getAllUsersWithAccesses(): List<ImmichUserAccesses> {
        return withTransaction {
            ImmichUsersTable.selectAll().map { it.toPersistedImmichUser() }.map { p ->
                val userMe = gson.fromJson(p.data, ImmichUserMe::class.java)
                val servers = ImmichAuthenticationTable.selectAll()
                    .where { ImmichAuthenticationTable.userId eq p.userId }
                    .map { it.toPersistedApiKey() }
                    .map { auth ->
                        val keyDets = gson.fromJson(auth.data, ImmichApiKeyPostResponseDto::class.java)
                        ImmichServerAccess(
                            keyName = keyDets.name,
                            keyId = keyDets.id.toString(),
                            serverUrl = auth.serverUrl,
                            isActive = auth.isActive,
                            createdAt = auth.createdAt,
                        )
                    }
                ImmichUserAccesses(user = userMe, isActive = p.isActive, servers = servers)
            }
        }.getOrDefault(emptyList())
    }

    fun deleteApiKey(apiKeyId: String): Int {
        return withTransaction {
            ImmichAuthenticationTable.deleteWhere { ImmichAuthenticationTable.apiKeyId eq apiKeyId }
        }.getOrDefault(0)
    }

    fun switchUser(userId: String): Boolean {
        return withTransaction {
            val userExists = ImmichUsersTable.selectAll().where { ImmichUsersTable.userId eq userId }.any()
            if (!userExists) return@withTransaction false

            // Deaktiver alle, aktiver den valgte
            ImmichUsersTable.update { it[isActive] = false }
            ImmichUsersTable.update({ ImmichUsersTable.userId eq userId }) { it[isActive] = true }
            true
        }.getOrDefault(false)
    }

    fun getUsers(): List<ImmichUserMe> {
        return withTransaction {
            ImmichUsersTable.select(ImmichUsersTable.data)
                .map { it[ImmichUsersTable.data] }
                .map { gson.fromJson(it, ImmichUserMe::class.java) }

        }.getOrDefault(emptyList())
    }
}