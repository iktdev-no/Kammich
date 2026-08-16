package no.iktdev.kammich.immich.exceptions

sealed class ImmichLoginException(message : String) : ImmichException(message)

class ImmichLoginIncorrectUsernameOrPasswordException(message : String) : ImmichLoginException(message)