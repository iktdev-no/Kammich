package no.iktdev.kammich.models.shared.system

data class PowerPermissionsDto(
    val canPowerOff: Boolean,
    val canReboot: Boolean
)

data class ActionResponse(
    val success: Boolean,
    val message: String
)