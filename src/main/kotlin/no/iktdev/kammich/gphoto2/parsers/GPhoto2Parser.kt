package no.iktdev.kammich.gphoto2.parsers

interface GPhoto2Parser<T> {
    fun parse(input: String): T
}