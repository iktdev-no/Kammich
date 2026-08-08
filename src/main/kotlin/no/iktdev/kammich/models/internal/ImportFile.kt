package no.iktdev.kammich.models.internal

import no.iktdev.kammich.models.shared.FileImportState

data class ImportFile(val file: KFile, val state: FileImportState)