package no.iktdev.kammich.models.shared.immich

import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPostResponseDto
import no.iktdev.kammich.models.shared.immich.api.ImmichUserMe

data class ImmichUserAccesses(
    val user: ImmichUserMe,
    val isActive: Boolean,
    val servers: List<ImmichServerAccess>,
) {
}

data class ImmichServerAccess(
    val keyName: String,
    val keyId: String,
    val serverUrl: String,
    val isActive: Boolean,
    val createdAt: String
)