package no.iktdev.kammich.immich

import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPost
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPostResponse
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPostResponseDto
import no.iktdev.kammich.models.shared.immich.api.ImmichAuthenticationLogin
import no.iktdev.kammich.models.shared.immich.api.ImmichAuthenticationLoginResponse
import no.iktdev.kammich.models.shared.immich.api.ImmichUserMe
import no.iktdev.kammich.utils.RestClientFactory
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.body
import java.util.UUID

class ImmichClient(
    serverUrl: String,
    restClientFactory: RestClientFactory
) : ImmichApi {

    private val client = restClientFactory.create(serverUrl)

    override fun login(payload: ImmichAuthenticationLogin): ImmichAuthenticationLoginResponse {
        return client.post()
            .uri("/api/auth/login")
            .body(payload)
            .retrieve()
            .body<ImmichAuthenticationLoginResponse>()
            ?: throw IllegalStateException("Klarte ikke å logge inn på Immich")
    }

    override fun logout(sessionToken: String) {
        client.post()
            .uri("/api/auth/logout")
            .header("x-immich-user-token", sessionToken)
            .retrieve()
            .toBodilessEntity()
    }

    override fun meBySession(sessionToken: String): ImmichUserMe {
        return client.get()
            .uri("/api/users/me")
            .header("x-immich-user-token", sessionToken)
            .retrieve()
            .body<ImmichUserMe>()
            ?: throw IllegalStateException("Klarte ikke å hente info om meg via sesjon")
    }

    override fun meByApiKey(apiKey: String): ImmichUserMe {
        return client.get()
            .uri("/api/users/me")
            .header("x-api-key", apiKey)
            .retrieve()
            .body<ImmichUserMe>()
            ?: throw IllegalStateException("Klarte ikke å hente info om meg via API-nøkkel")
    }

    override fun createApiKey(sessionToken: String, payload: ImmichApiKeyPost): ImmichApiKeyPostResponse {
        return client.post()
            .uri("/api/api-keys")
            .header("x-immich-user-token", sessionToken)
            .body(payload)
            .retrieve()
            .body<ImmichApiKeyPostResponse>()
            ?: throw IllegalStateException("Klarte ikke å generere API-nøkkel fra Immich")
    }

    override fun getMyApiKey(apiKey: String): ImmichApiKeyPostResponseDto {
        return client.get()
            .uri("/api/api-keys/me")
            .header("x-api-key", apiKey)
            .retrieve()
            .body<ImmichApiKeyPostResponseDto>()
            ?: throw IllegalStateException("Klarte ikke å hente info om meg via API-nøkkel")
    }

    override fun getProfileImage(apiKey: String, userId: UUID): ByteArray {
        return client.get()
            .uri("/api/users/$userId/profile-image")
            .header("x-api-key", apiKey)
            .accept(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.APPLICATION_OCTET_STREAM)
            .retrieve()
            .body<ByteArray>() ?: throw IllegalStateException("Klarte ikke å hente profilbilde")
    }
}