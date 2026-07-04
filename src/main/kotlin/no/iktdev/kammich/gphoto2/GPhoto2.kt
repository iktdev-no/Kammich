package no.iktdev.kammich.gphoto2

import no.iktdev.kammich.gphoto2.model.GPhoto2DeviceAbility
import no.iktdev.kammich.gphoto2.model.GPhoto2DiscoveredDevice
import no.iktdev.kammich.gphoto2.model.GPhoto2File
import no.iktdev.kammich.gphoto2.model.GPhoto2Summary
import no.iktdev.kammich.gphoto2.model.GPhoto2Device
import no.iktdev.kammich.gphoto2.parsers.GPhoto2AbilityParser
import no.iktdev.kammich.gphoto2.parsers.GPhoto2ConnectedDeviceParser
import no.iktdev.kammich.gphoto2.parsers.GPhoto2FileListParser
import no.iktdev.kammich.gphoto2.parsers.GPhoto2SummaryParser
import no.iktdev.kammich.storage.DeviceManagerService
import org.slf4j.LoggerFactory
import java.io.File

class GPhoto2: IGPhoto2 {
    private val log = LoggerFactory.getLogger(DeviceManagerService::class.java)

    override fun execute(vararg args: String): String {
        log.info("Executing gphoto2 with ${args.joinToString(" ")}")
        return ProcessBuilder("gphoto2", *args)
            .start()
            .inputStream
            .bufferedReader()
            .readText()
    }

    override fun copyFile(
        device: GPhoto2DiscoveredDevice,
        file: GPhoto2File,
        destination: File,
        onProgress: (Int) -> Unit
    ) {
        val builder = GPhoto2CommandBuilder()
            .port(device.port)
            .copy(destination, file)

        val process = ProcessBuilder("gphoto2", *builder.build())
            .redirectErrorStream(false) // Viktig: Hold stderr adskilt for progresjon
            .start()

        // 1. Les stderr for progresjon
        val stderrThread = Thread {
            process.errorStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
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
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw CopyException("Feil ved nedlasting, exit kode: $exitCode")
        }
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

    override fun getDevices(): List<GPhoto2Device> {
        return discover().map { dd ->
            val ability = getAbilities(dd)
            val summary = getSummary(dd)
            GPhoto2Device(dd.port, ability, summary)
        }
    }

    override fun getDeviceInfo(port: String): GPhoto2Device {
        return GPhoto2Device(port, getAbilities(port), getSummary(port))
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

    // I GPhoto2-klassen
    override fun getThumbnail(cachePath: String, device: GPhoto2DiscoveredDevice, file: GPhoto2File): File {
        val cacheFolder = File(cachePath)
        val cacheFile = File(cacheFolder, "${file.name.hashCode()}.jpg")

        // 1. Sjekk cache først
        if (cacheFile.exists()) return cacheFile

        // 2. Hvis ikke, hent fra kamera (gphoto2)
        cacheFolder.mkdirs()
        val builder = GPhoto2CommandBuilder()
            .port(device.port)
            // Bruk builder-metoden vi laget
            .getThumbnail(cacheFile, file)

        val process = ProcessBuilder("gphoto2", *builder.build()).start()
        if (process.waitFor() != 0) throw CopyException("Kunne ikke hente thumbnail")

        return cacheFile
    }

    override fun getFiles(port: String, path: String?): List<GPhoto2File> {
        val targetPath = if (path.isNullOrBlank()) "/" else path
        log.info("getFiles path: $targetPath")
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

    class CopyException(override val message: String?): Exception()
}