package no.iktdev.kammich.models

data class FileHash(val hash: String, val method: FileHashType) {
}

enum class FileHashType {
    XX64Hash,
    SHA1
}