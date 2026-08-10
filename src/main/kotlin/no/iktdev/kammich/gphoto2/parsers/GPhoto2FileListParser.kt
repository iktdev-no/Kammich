package no.iktdev.kammich.gphoto2.parsers

import no.iktdev.kammich.gphoto2.model.GPhoto2File
import no.iktdev.kammich.gphoto2.model.GPhoto2NodeType
import org.slf4j.LoggerFactory

class GPhoto2FileListParser : GPhoto2Parser<List<GPhoto2File>> {
    private val log = LoggerFactory.getLogger(GPhoto2FileListParser::class.java)

    private val fileRegex = Regex("""FILENAME='([^']+)'\s+.*?FILESIZE=(\d+)\s+.*?FILETYPE=(\S+)""")

    override fun parse(input: String): List<GPhoto2File> {
        val nodes = mutableListOf<GPhoto2File>()

        input.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach

            when {
                // Hvis linjen starter med '/', betyr det at det er en absolutt sti (enten mappe eller fil)
                trimmed.startsWith("/") && !trimmed.contains("FILENAME=") -> {
                    val name = trimmed.substringAfterLast("/")
                    log.info("Found folder: $trimmed with name $name")
                    if (name.isNotEmpty()) {
                        nodes.add(GPhoto2File(
                            name = name,
                            type = GPhoto2NodeType.FOLDER,
                            folderPath = trimmed
                        ))
                    }
                }
                // Hvis linjen er en fil (enten i --parsable format med FILENAME= eller rå filsti)
                trimmed.contains("FILENAME=") || (trimmed.startsWith("/") && trimmed.contains(".")) -> {
                    val match = fileRegex.find(trimmed)
                    if (match != null) {
                        val (fullFilePath, sizeStr, mimeType) = match.destructured

                        val folderPath = fullFilePath.substringBeforeLast("/").ifEmpty { "/" }
                        val rawName = fullFilePath.substringAfterLast("/")

                        nodes.add(GPhoto2File(
                            name = sanitizeFileName(rawName, mimeType),
                            type = GPhoto2NodeType.FILE,
                            folderPath = folderPath,
                            sizeBytes = sizeStr.toLong()
                        ))
                    }
                }
            }
        }

        return nodes.distinctBy { "${it.folderPath}/${it.name}" }
    }

    val enforcedExtension = listOf(
        GFile("image/jpeg", "jpg"),
        GFile("image/png", "png")
    )

    private fun sanitizeFileName(name: String, mimeType: String): String {
        val eex = enforcedExtension.find { it.mimetype.equals(mimeType, ignoreCase = true) }
        if (eex != null) {
            if (name.substringAfter(".") != eex.extension) {
                val newName = name.substringBeforeLast(".") + ".${eex.extension}"
                log.info("Vasket filnavn: Endrer '{}' til '{}' (Mime: {})", name, newName, mimeType)
                return newName
            }
        }
        return name
    }

    data class GFile(val mimetype: String, val extension: String)
}