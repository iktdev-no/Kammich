package no.iktdev.kammich.immich.mapper

import no.iktdev.kammich.immich.models.ApiKeyCreateDto
import no.iktdev.kammich.immich.models.Permission
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPost
import no.iktdev.kammich.models.shared.immich.api.ImmichApiKeyPostResponse

fun ImmichApiKeyPost.fromDomain(): ApiKeyCreateDto {
    return ApiKeyCreateDto(
        name = this.name,
        permissions = this.permissions.map {
            // Bruk Permission.decode for å slå opp via verdi/SerializedName istedenfor valueOf
            Permission.decode(it) ?: throw IllegalArgumentException("Ukjent permission: $it")
        },
    )
}