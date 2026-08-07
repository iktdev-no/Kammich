package no.iktdev.kammich.storage.provider

class ImportException(override val message: String?): Exception()

class DeviceUnavailableException(override val message: String?): Exception()