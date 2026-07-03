package no.iktdev.kammich.gphoto2.parsers

import no.iktdev.kammich.gphoto2.model.GPhoto2File
import no.iktdev.kammich.gphoto2.model.GPhoto2NodeType

class GPhoto2FileListParser : GPhoto2Parser<List<GPhoto2File>> {
    // Den nye Regex-en:
// #(\d+)       -> ID
// \s+(.+?)     -> Navn (Lazy match)
// (?:\s+rd)?   -> Ignorerer "rd" hvis det finnes (valgfritt)
// \s+(\d+)     -> Størrelse
// \s+KB        -> Enheten
// \s+(\S+)     -> MimeType
    private val fileRegex = Regex("""#(\d+)\s+(.+?)(?:\s+rd)?\s+(\d+)\s+KB\s+(\S+)""")

    override fun parse(input: String): List<GPhoto2File> {
        val nodes = mutableListOf<GPhoto2File>()
        var currentFolder = ""

        input.lines().forEach { line ->
            val trimmed = line.trim()

            when {
                trimmed.startsWith("There are") || trimmed.startsWith("There is") -> {
                    currentFolder = trimmed.substringAfter("folder '").substringBefore("'")
                    nodes.add(GPhoto2File(currentFolder.substringAfterLast("/"), GPhoto2NodeType.FOLDER, folderPath = currentFolder))
                }
                trimmed.startsWith("- ") -> {
                    val name = trimmed.removePrefix("- ").trim()
                    // Hvis vi står i "/", bli "/navn". Hvis vi står i "/A", bli "/A/navn"
                    val folderPath = if (currentFolder == "/") "/$name" else "$currentFolder/$name"
                    nodes.add(GPhoto2File(name, GPhoto2NodeType.FOLDER, folderPath = folderPath))
                }
                trimmed.startsWith("#") -> {
                    val match = fileRegex.find(trimmed)
                    if (match != null) {
                        // Nå matcher antall variabler nøyaktig antall grupper i Regex-en
                        val (id, name, size, mime) = match.destructured
                        nodes.add(GPhoto2File(
                            name = name.trim(),
                            type = GPhoto2NodeType.FILE,
                            folderPath = currentFolder,
                            sizeBytes = size.toLong() * 1024,
                            mimeType = mime
                        ))
                    }
                }
            }
        }
        return nodes
    }
}