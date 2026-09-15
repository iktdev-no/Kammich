package no.iktdev.kammich.immich.client

import no.iktdev.kammich.immich.ImmichApi
import no.iktdev.kammich.utils.RestClientFactory
import org.springframework.stereotype.Component

@Component
class ImmichClientFactory(
) {
    fun create(serverUrl: String): ImmichApi {
        val cleanUrl = serverUrl.trim().removeSuffix("/") + "/api"
        return ImmichClient(cleanUrl)
    }
}