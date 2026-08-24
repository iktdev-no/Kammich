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
        //log.info("Gphoto2 raw:\n{}", result)
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

        val arguments = GPhoto2CommandBuilder()
            .port(port)
            .copy(targetFile, containingFolder, fileName)
            .build()

        log.info(
            "Copying file $fileName with arguments: " +
                    "gphoto2 ${arguments.joinToString(" ")}"
        )

        val process = ProcessBuilder("gphoto2", *arguments)
            .redirectErrorStream(false)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()

        val errorOutput = StringBuilder()

        val stderrThread = Thread {
            process.errorStream.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    log.error("gphoto2: $line")
                    errorOutput.append(line).append('\n')

                    Regex("""(\d+)%""")
                        .find(line)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull()
                        ?.let(onProgress)
                }
            }
        }

        stderrThread.start()

        log.info("Waiting for copy of $fileName to $destination for completion")

        val exitCode = process.waitFor()

        // Sørg for at hele stderr faktisk er lest
        stderrThread.join()

        val error = errorOutput.toString().trim()

        log.info("gphoto2 finished with exitCode=$exitCode")
        if (error.isNotEmpty()) {
            log.info("gphoto2 stderr: $error")
        }

        if (exitCode != 0) {
            val isDeviceUnavailable =
                error.contains("No camera found", ignoreCase = true) ||
                        error.contains("Could not claim the USB device", ignoreCase = true) ||
                        error.contains("I/O error", ignoreCase = true) ||
                        error.contains("Could not open port", ignoreCase = true)

            if (isDeviceUnavailable) {
                throw DeviceUnavailableException(
                    "Kamera mistet eller utilgjengelig: $error"
                )
            }

            throw ImportException(
                "Feil ved nedlasting, exit kode: $exitCode. Detaljer:\n$error"
            )
        }

        if (!targetFile.exists()) {
            throw ImportException(
                "gphoto2 rapporterte suksess (exit code 0), " +
                        "men filen eksisterer ikke etter nedlasting: $targetFile"
            )
        }

        return targetFile
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