package no.iktdev.kammich.gphoto2

import no.iktdev.kammich.gphoto2.model.GPhoto2File
import java.io.File

class GPhoto2CommandBuilder {
    private val args = mutableListOf<String>()

    fun autoDetect() = apply { args.add("--auto-detect") }
    fun port(port: String) = apply { args.add("--port"); args.add(port) }
    fun abilities() = apply { args.add("--abilities") }
    fun summary() = apply { args.add("--summary") }

    fun listRoot() = apply {
        explore("/")
    }

    // Utforsker en spesifikk sti
    fun explore(path: String) = apply {
        args.add("--folder")
        args.add(path)
        // Her kjører vi to operasjoner på samme path
        args.add("--list-folders")
        args.add("--list-files")
        args.add("--no-recurse")
    }

    fun copy(destination: File, file: GPhoto2File) = apply {
        args.add("--folder")
        args.add(file.folderPath)
        args.add("--get-file")
        args.add(file.name)
        // gphoto2 bruker standard output for filen hvis vi ikke spesifiserer --filename
        // Men det er lurt å sette arbeidsmappen eller destinasjonen
        args.add("--filename")
        args.add("${destination.absolutePath}/${file.name}")
    }

    fun delete(file: GPhoto2File) = apply {
        args.add("--folder")
        args.add(file.folderPath)
        args.add("--delete-file")
        args.add(file.name)
    }

    fun build() = args.toTypedArray()
}

// Et lite hjelpeobjekt for å kjøre kommandoen
class GPhoto2Command(private val args: Array<String>, private val executor: (Array<String>) -> String) {
    fun run(): String = executor(args)
}