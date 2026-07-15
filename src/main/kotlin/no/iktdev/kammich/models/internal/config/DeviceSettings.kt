package no.iktdev.kammich.models.internal.config

import no.iktdev.kammich.models.shared.DeviceSettingsDto

data class DeviceSettings(
    var autoImport: Boolean = true,
    var includeFolders: List<String> = emptyList(),
    var excludeFolders: List<String> = emptyList()
) {
    fun toDto() = DeviceSettingsDto(
        autoImport = autoImport,
        includeFolders = includeFolders,
        excludeFolders = excludeFolders
    )

    // Fra Frontend til Backend
    fun apply(dto: DeviceSettingsDto) {
        dto.autoImport?.let { this.autoImport = it }
        dto.includeFolders?.let { this.includeFolders = it }
        dto.excludeFolders?.let { this.excludeFolders = it }
    }
}