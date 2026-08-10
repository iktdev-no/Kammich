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



data class ImportFile(
    val file: String,
    val isNew: Boolean,
    val state: FileImportState = FileImportState.Pending
)

data class DeviceImportSummary(
    val deviceId: String,
    val state: ImportState = ImportState.Indexing,
    val started: String,
    val completed: String,
)

enum class ImportState {
    Indexing,
    Importing,
    Completed,
    Canceled
}



data class ImportProgressEvent(
    val deviceId: String,
    val completedFiles: Int,
    val totalFiles: Int,
    val currentFile: String?,
    val state: FileImportState, // F.eks. InProgress, Completed, Failed
    val files: List<ImportFile> // Eller bare den filen som oppdaterte seg
)