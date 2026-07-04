package no.iktdev.kammich

fun String.toMD5(): String {
    return this.toByteArray().let {
        java.security.MessageDigest.getInstance("MD5").digest(it)
            .joinToString("") { b -> "%02x".format(b) }
    }
}