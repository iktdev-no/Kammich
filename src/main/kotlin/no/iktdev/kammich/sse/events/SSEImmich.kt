package no.iktdev.kammich.sse.events

import no.iktdev.kammich.models.shared.immich.ImmichAvailability
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPostResponseDto
import no.iktdev.kammich.models.shared.immich.api.ImmichUserMe
import no.iktdev.kammich.sse.ISSE

data class SSEImmichUser(val payload: ImmichUserMe?): ISSE {
    override val type = "immich-user-me"
}

data class SSEImmichApiKeyInUse(val payload: ImmichApiKeyPostResponseDto): ISSE {
    override val type = "immich-api-key-in-use"
}

data class SSEImmichAvailability(val payload: ImmichAvailability): ISSE {
    override val type = "immich-availability"
}