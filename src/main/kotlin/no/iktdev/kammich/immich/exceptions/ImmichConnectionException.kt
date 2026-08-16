package no.iktdev.kammich.immich.exceptions

sealed class ImmichConnectionException(override val message: String): ImmichException(message) {
}

class ImmichConnectionUnavailableException(message: String) : ImmichConnectionException(message)