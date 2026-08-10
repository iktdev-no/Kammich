package no.iktdev.kammich.immich

import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPost
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPostResponse
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPostResponseDto
import no.iktdev.kammich.models.shared.immich.api.ImmichAuthenticationLogin
import no.iktdev.kammich.models.shared.immich.api.ImmichAuthenticationLoginResponse
import no.iktdev.kammich.models.shared.immich.api.ImmichUserMe
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import java.util.UUID

interface ImmichApi {
    fun login(payload: ImmichAuthenticationLogin): ImmichAuthenticationLoginResponse
    fun logout(sessionToken: String)

    // Hent brukerinfo via aktiv sesjonstoken
    fun meBySession(sessionToken: String): ImmichUserMe

    // Hent brukerinfo via permanent API-nøkkel
    fun meByApiKey(apiKey: String): ImmichUserMe

    fun createApiKey(sessionToken: String, payload: ImmichApiKeyPost): ImmichApiKeyPostResponse
    fun getMyApiKey(apiKey: String): ImmichApiKeyPostResponseDto

    fun getProfileImage(apiKey: String, userId: UUID): ByteArray
}