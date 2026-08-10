package no.iktdev.kammich.immich

import no.iktdev.kammich.utils.RestClientFactory
import org.springframework.stereotype.Component

@Component
class ImmichClientFactory(
    private val restClientFactory: RestClientFactory
) {
    fun create(serverUrl: String): ImmichApi {
        val cleanUrl = serverUrl.trim().removeSuffix("/")
        return ImmichClient(cleanUrl, restClientFactory)
    }
}