package no.iktdev.kammich.gphoto2

import no.iktdev.kammich.gphoto2.model.GPhoto2DeviceAbility
import no.iktdev.kammich.gphoto2.model.GPhoto2DiscoveredDevice
import no.iktdev.kammich.gphoto2.model.GPhoto2File
import no.iktdev.kammich.gphoto2.model.GPhoto2Summary
import no.iktdev.kammich.gphoto2.model.GPhoto2DeviceInfo
import no.iktdev.kammich.gphoto2.parsers.GPhoto2AbilityParser
import no.iktdev.kammich.gphoto2.parsers.GPhoto2ConnectedDeviceParser
import no.iktdev.kammich.gphoto2.parsers.GPhoto2FileListParser
import no.iktdev.kammich.gphoto2.parsers.GPhoto2SummaryParser
import no.iktdev.kammich.storage.DeviceManagerService
import no.iktdev.kammich.storage.provider.DeviceUnavailableException
import no.iktdev.kammich.storage.provider.ImportException
import org.slf4j.LoggerFactory
import java.io.File

class GPhoto2: IGPhoto2 {
    private val log = LoggerFactory.getLogger(DeviceManagerService::class.java)

    override fun getPort(sysPath: String): String {
        val bus = File("$sysPath/busnum").readText().trim().toInt()
        val dev = File("$sysPath/devnum").readText().trim().toInt()
        return "usb:%03d,%03d".format(bus, dev)
    }

    override fun execute(vararg args: String): String {
        val result = ProcessBuilder("gphoto2", *args)
            .start()
            .inputStream
            .bufferedReader()
            .readText()
        log.info("Gphoto2 raw:\n{}", result)
        return result
    }

    override fun copyFile(
        port: String,
        containingFolder: String,
        fileName: String,
        destination: File,
        onProgress: (Int) -> Unit
    ): File {
        val targetFile = File(destination, fileName)
        if (targetFile.exists()) {
            targetFile.delete()
        }
        val builder = GPhoto2CommandBuilder()
            .port(port)
            .copy(targetFile, containingFolder, fileName)

        val process = ProcessBuilder("gphoto2", *builder.build())
            .redirectErrorStream(false) // Viktig: Hold stderr adskilt for progresjon
            .start()

        val errorOutput = StringBuilder()

        // 1. Les stderr for progresjon
        val stderrThread = Thread {
            process.errorStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    log.error(line)
                    errorOutput.append(line).append("\n")
                    // Regex for å finne prosenten, f.eks "Downloading: 45%"
                    val match = Regex("""(\d+)%""").find(line)
                    if (match != null) {
                        val percent = match.groupValues[1].toInt()
                        onProgress(percent)
                    }
                }
            }
        }
        stderrThread.start()

        // 2. Vent på at prosessen fullføres
        log.info("Waiting for copy of $fileName to $destination for completion")
        val exitCode = process.waitFor()

        if (exitCode == 1) {
            val cleanError = errorOutput.toString().replace(Regex("\\s+"), " ").trim()
            val isDeviceUnavailable = (
                    cleanError.contains("No camera found", ignoreCase = true) ||
                            cleanError.contains("Could not claim the USB device", ignoreCase = true) ||
                            cleanError.contains("I/O error", ignoreCase = true) ||
                            cleanError.contains("Could not open port", ignoreCase = true)
                    )

            if (isDeviceUnavailable) {
                throw DeviceUnavailableException("Kamera mistet eller utilgjengelig: $cleanError")
            }
        }

        if (exitCode != 0) {
            throw ImportException("Feil ved nedlasting, exit kode: $exitCode. Detaljer: \n$errorOutput")
        }
        return File(destination, fileName)
    }

    override fun deleteFile(device: GPhoto2DiscoveredDevice, file: GPhoto2File): Boolean {
        val builder = GPhoto2CommandBuilder()
            .port(device.port)
            .delete(file)

        // Vi bruker en enkel run her, siden sletting sjelden returnerer
        // noe annet enn suksess/feil i exit-koden.
        val process = ProcessBuilder("gphoto2", *builder.build()).start()
        val exitCode = process.waitFor()

        return exitCode == 0
    }

    override fun getDevices(): List<GPhoto2DeviceInfo> {
        return discover().map { dd ->
            val ability = getAbilities(dd)
            val summary = getSummary(dd)
            GPhoto2DeviceInfo( ability, summary, isReady = summary.storageDevices.isNotEmpty())
        }
    }

    override fun getDeviceInfo(port: String): GPhoto2DeviceInfo {
        val summary = getSummary(port)
        return GPhoto2DeviceInfo(getAbilities(port), summary, isReady = summary.storageDevices.isNotEmpty())
    }

    override fun discover(): List<GPhoto2DiscoveredDevice> {
        val out = gphoto { autoDetect() }.run()
        return GPhoto2ConnectedDeviceParser().parse(out)
    }

    override fun getAbilities(device: GPhoto2DiscoveredDevice): GPhoto2DeviceAbility {
        val out = gphoto {
            port(device.port)
            abilities()
        }.run()
        return GPhoto2AbilityParser().parse(out)
    }

    override fun getSummary(device: GPhoto2DiscoveredDevice): GPhoto2Summary {
        val out = gphoto {
            port(device.port)
            summary() // Husk å legge til denne i CommandBuilder!
        }.run()

        return GPhoto2SummaryParser().parse(out)
    }

    override fun getAbilities(port: String): GPhoto2DeviceAbility {
        val out = gphoto {
            port(port)
            abilities()
        }.run()
        return GPhoto2AbilityParser().parse(out)
    }

    override fun getSummary(port: String): GPhoto2Summary {
        val out = gphoto {
            port(port)
            summary() // Husk å legge til denne i CommandBuilder!
        }.run()

        return GPhoto2SummaryParser().parse(out)
    }

    override fun getThumbnails(cacheDirectory: File, port: String, folder: String, recurse: Boolean): List<File> {
        // 1. Sørg for at mappen finnes
        cacheDirectory.mkdirs()

        // 2. Kjør gphoto2-kommandoen
        val builder = GPhoto2CommandBuilder()
            .port(port)
            .getThumbnail(cacheDirectory, folder, recurse)

        val process = ProcessBuilder("gphoto2", *builder.build())
            .redirectErrorStream(true)
            .start()

        if (process.waitFor() != 0) {
            throw ImportException(
                "Kunne ikke synkronisere thumbnails: ${
                    process.inputStream.bufferedReader().readText()
                }"
            )
        }

        // 3. Poenget: GPhoto2 har nå dumpet alle .jpg-filene i cacheDirectory.
        // Vi returnerer bare alle filene som ligger der nå.
        return cacheDirectory.listFiles { _, name -> name.endsWith(".jpg") }?.toList() ?: emptyList()
    }

    override fun getFiles(port: String, path: String?): List<GPhoto2File> {
        val targetPath = if (path.isNullOrBlank()) "/" else path
        log.debug("Fishing for files in $targetPath")
        val out = gphoto {
            port(port)
            explore(targetPath) // Bruk explore() for alle stier
        }

        return GPhoto2FileListParser().parse(out.run())
    }

    private fun gphoto(block: GPhoto2CommandBuilder.() -> Unit): GPhoto2Command {
        val builder = GPhoto2CommandBuilder().apply(block)
        return GPhoto2Command(builder.build()) { args -> execute(*args) }
    }
}