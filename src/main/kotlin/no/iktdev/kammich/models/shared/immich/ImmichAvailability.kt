package no.iktdev.kammich.models.shared.immich

import no.iktdev.kammich.models.shared.immich.api.ImmichUserMe

data class ImmichAvailability(
    val serverUrl: String?,
    val isAvailable: Boolean,
    val user: ImmichUserMe? = null,
    val error: String? = null
)