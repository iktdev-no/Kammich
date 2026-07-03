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
import java.io.File

class GPhoto2: IGPhoto2 {
    override fun execute(vararg args: String): String {
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
            GPhoto2Device(dd, ability, summary)
        }
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

    fun getFiles(device: GPhoto2DiscoveredDevice, file: GPhoto2File? = null): List<GPhoto2File> {
        val out = (if (file != null) {
            gphoto {
                port(device.port)
                explore(file.folderPath)
            }
        } else {
            gphoto {
                port(device.port)
                listRoot()
            }
        }).run()
        return GPhoto2FileListParser().parse(out)
    }


    private fun gphoto(block: GPhoto2CommandBuilder.() -> Unit): GPhoto2Command {
        val builder = GPhoto2CommandBuilder().apply(block)
        return GPhoto2Command(builder.build()) { args -> execute(*args) }
    }

    class CopyException(override val message: String?): Exception()
}