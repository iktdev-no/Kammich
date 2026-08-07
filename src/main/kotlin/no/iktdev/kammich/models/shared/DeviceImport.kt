package no.iktdev.kammich.models.shared

import java.time.Instant

data class DeviceImport(
    val deviceId: String,
    val deviceName: String,
    val started: Instant,
    val totalFiles: Int,
    val completedFiles: Int, // Hvor mange som er ferdig (Importere + Failed)
    val currentFileName: String?, // F.eks. den som lastes ned akkurat nå
    val files: List<ImportFile>
)

data class ImportProgressEvent(
    val deviceId: String,
    val completedFiles: Int,
    val totalFiles: Int,
    val currentFile: String?,
    val state: ImportState, // F.eks. InProgress, Completed, Failed
    val files: List<ImportFile> // Eller bare den filen som oppdaterte seg
)

data class ImportFile(
    val file: String,
    val isNew: Boolean,
    val state: ImportState = ImportState.Pending
)

enum class ImportState {
    Pending,
    Imported,
    Failed
}

