package no.iktdev.kammich.models.shared

data class DeviceSettingsDto(
    var autoImport: Boolean?,
    var deleteWhenVerifiedBackedup: Boolean?,
    var includeFolders: List<String>?,
    var excludeFolders: List<String>?
)