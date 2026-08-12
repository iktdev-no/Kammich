package no.iktdev.kammich.database.models

import java.util.UUID

data class PersistedDeviceOwner(
    val deviceSerialNumber: String,
    val immichUserId: UUID,
)