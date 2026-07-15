package no.iktdev.kammich.system.exceptions

class TetherDeviceNotEnabledException(override val message: String, val deviceId: String? = null) : Exception(message)
class TetherDeviceNotFoundException(override val message: String) : Exception(message)