package no.iktdev.kammich.gphoto2.parsers

import com.google.gson.Gson
import no.iktdev.kammich.gphoto2.model.GPhoto2File
import no.iktdev.kammich.gphoto2.model.GPhoto2NodeType
import no.iktdev.kammich.storage.DeviceManagerService
import org.slf4j.LoggerFactory

class GPhoto2FileListParser : GPhoto2Parser<List<GPhoto2File>> {
    private val log = LoggerFactory.getLogger(GPhoto2FileListParser::class.java)

    // Den nye Regex-en:
// #(\d+)       -> ID
// \s+(.+?)     -> Navn (Lazy match)
// (?:\s+rd)?   -> Ignorerer "rd" hvis det finnes (valgfritt)
// \s+(\d+)     -> Størrelse
// \s+KB        -> Enheten
// \s+(\S+)     -> MimeType
    private val fileRegex = Regex("""#(\d+)\s+(.+?)(?:\s+rd)?\s+(\d+)\s+KB\s+(\d+x\d+)\s+(\S+)\s(\d+)""")

    override fun parse(input: String): List<GPhoto2File> {
        val nodes = mutableListOf<GPhoto2File>()
        var currentFolder = ""

        input.lines().forEach { line ->
            val trimmed = line.trim()

            when {
                trimmed.startsWith("There are") || trimmed.startsWith("There is") -> {
                    // Bare oppdater stien. IKKE legg til node her!
                    currentFolder = trimmed.substringAfter("folder '").substringBefore("'")
                }
                trimmed.startsWith("- ") -> {
                    val name = trimmed.removePrefix("- ").trim()

                    // Håndter stier korrekt
                    val folderPath = if (currentFolder == "/") "/$name" else "$currentFolder/$name"

                    nodes.add(GPhoto2File(
                        name = name,
                        type = GPhoto2NodeType.FOLDER,
                        folderPath = folderPath
                    ))
                }
                trimmed.startsWith("#") -> {
                    val match = fileRegex.find(trimmed)
                    if (match != null) {
                        val (id, name, size, _, mimeType, timestamp) = match.destructured
                        nodes.add(GPhoto2File(
                            name = sanitizeFileName(name.trim(), mimeType),
                            type = GPhoto2NodeType.FILE,
                            folderPath = currentFolder,
                            sizeBytes = size.toLong() * 1024
                        ))
                    }
                }
            }
        }

        // Hvis vi er på toppen og listen er tom, kan vi legge til et eksplisitt Root-element
        // eller bare la UI-et håndtere at en tom liste betyr "tomt"
        return nodes.distinctBy { "${it.folderPath}/${it.name}" }
    }

    val enforcedExtension = listOf(
        GFile("image/jpeg", "jpg"),
        GFile("image/png", "png")
    )

    private fun sanitizeFileName(name: String, mimeType: String): String {
        val eex = enforcedExtension.find { it -> it.mimetype.equals(mimeType, ignoreCase = true) }
        if (eex != null) {
            if (name.substringAfter(".") != eex.extension) {
                val newName = name.substringBeforeLast(".") + ".${eex.extension}"
                log.info("Vasket filnavn: Endrer '{}' til '{}' (Mime: {})", name, newName, mimeType)
                return newName
            }
        } else {
            log.info("Vasket ikke filnavn.. $name, $mimeType")
        }
        return name
    }

    data class GFile(val mimetype: String, val extension: String)
}