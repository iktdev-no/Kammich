package no.iktdev.kammich.models.shared

import java.util.UUID

data class DeviceClaim(
    val deviceSN: String,
    val claimedByUserId: UUID,
)