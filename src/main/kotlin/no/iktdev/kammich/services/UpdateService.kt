package no.iktdev.kammich.services

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import no.iktdev.kammich.models.shared.update.AppUpdateProgress
import no.iktdev.kammich.models.shared.update.AppUpdateStatus
import no.iktdev.kammich.sse.SseManager
import no.iktdev.kammich.sse.events.SSEAppUpdater
import no.iktdev.kammich.system.SysCommand
import no.iktdev.kammich.toSha256
import org.springframework.stereotype.Service
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicBoolean

@Service
class UpdateService(
    private val exec: SysCommand,
    private val versionService: VersionService,
    private val sseManager: SseManager
) {

    @PostConstruct
    fun initialize() {
        sendStatus(AppUpdateStatus.None)
    }

    private val updateScope =
        CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val updating = AtomicBoolean(false)

    fun startUpdate(): Boolean {
        if (!updating.compareAndSet(false, true)) {
            return false
        }

        updateScope.launch {
            try {
                if (checkForUpdate()) {
                    update()
                }
            } finally {
                updating.set(false)
            }
        }

        return true
    }


    fun checkForUpdate(): Boolean =
        runInformed(AppUpdateStatus.Checking) {
            message("Sjekker etter oppdateringer...")

            val available = versionService.isUpdateAvailable()
            val release = versionService.getLatestRelease()

            sendStatus(
                if (available) {
                    AppUpdateStatus.UpdateAvailable
                } else {
                    AppUpdateStatus.None
                },
                version = release?.tagName
            )

            available
        }

    fun update(): Boolean {
        val runningJar = versionService.getRunningJar()
            ?: return failed("Kammich kjører ikke fra en oppdaterbar JAR.")

        val release = versionService.getLatestRelease()
            ?: return failed("Kunne ikke hente siste release.")

        return try {
            val version = release.tagName.removePrefix("v")

            val jar = release.findAsset("Kammich-v$version.jar")
                ?: return failed("Fant ikke JAR-filen i releasen.")

            val sha = release.findAsset("Kammich-v$version.jar.sha256")
                ?: return failed("Fant ikke SHA-256-filen i releasen.")

            val tempJar = downloadJar(jar, runningJar)
            val expectedSha = downloadSha(sha)

            verify(tempJar, expectedSha)
            replaceJar(tempJar, runningJar)

            restartService()

        } catch (e: Exception) {
            failed(e.message ?: "Ukjent feil under oppdatering.")
        }
    }

    private fun downloadJar(
        asset: GitHubReleaseService.GitHubReleaseAsset,
        runningJar: File
    ): File = runInformed(AppUpdateStatus.Downloading) {

        val destination = File(
            runningJar.parentFile,
            "${runningJar.name}.download"
        )

        message("Laster ned oppdatering...")
        progress(0)

        val connection = URI(asset.downloadUrl).toURL().openConnection()
        val size = connection.contentLengthLong

        connection.getInputStream().use { input ->
            destination.outputStream().use { output ->
                val buffer = ByteArray(1024 * 1024)
                var downloaded = 0L
                var lastProgress = -1
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead

                    if (size > 0) {
                        val currentProgress =
                            ((downloaded * 100) / size).toInt()

                        if (currentProgress != lastProgress) {
                            lastProgress = currentProgress
                            progress(currentProgress)
                        }
                    }
                }
            }
        }

        progress(100)
        destination
    }

    private fun downloadSha(
        asset: GitHubReleaseService.GitHubReleaseAsset
    ): String = runInformed(AppUpdateStatus.Downloading) {

        message("Laster ned SHA-256...")

        URI(asset.downloadUrl)
            .toURL()
            .readText()
            .trim()
            .substringBefore(" ")
    }

    private fun verify(
        file: File,
        expectedSha: String
    ): Boolean = runInformed(AppUpdateStatus.Verifying) {

        message("Verifiserer SHA-256...")

        val actualSha = file.toSha256()

        if (!actualSha.equals(expectedSha, true)) {
            error("SHA-256-verifisering feilet.")
        }

        true
    }

    private fun replaceJar(
        tempJar: File,
        runningJar: File
    ) = runInformed(AppUpdateStatus.Replacing) {

        message("Installerer oppdatering...")

        Files.move(
            tempJar.toPath(),
            runningJar.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }

    private fun restartService(): Boolean =
        runInformed(AppUpdateStatus.Restarting) {
            message("Starter Kammich på nytt...")

            val result = exec.sudo(
                "systemctl",
                "restart",
                "kammich-backend.service"
            )

            if (result.isFailure()) {
                result.fold { output, errOutput, exitCode ->
                    val error = errOutput
                        ?.takeIf { it.isNotBlank() }
                        ?: output

                    throw RuntimeException(
                        "Unable to restart Kammich service.\n" +
                                "Failed with exit code $exitCode: $error\n" +
                                "Ensure that you are running with the correct user and/or sudo right is correctly set"
                    )
                }
            }

            true
        }

    private inner class UpdateInformer(
        private val status: AppUpdateStatus
    ) {

        fun message(message: String) =
            sendStatus(status, message = message)

        fun progress(progress: Int) =
            sendStatus(status, progress = progress)

        fun update(
            progress: Int? = null,
            message: String? = null
        ) = sendStatus(
            status,
            progress = progress,
            message = message
        )
    }

    private inline fun <T> runInformed(
        status: AppUpdateStatus,
        block: UpdateInformer.() -> T
    ): T {
        val informer = UpdateInformer(status)

        return try {
            informer.block()
        } catch (e: Exception) {
            sendStatus(
                AppUpdateStatus.Failed,
                error = e.message ?: "Ukjent feil."
            )

            throw e
        }
    }

    private fun failed(message: String): Boolean {
        sendStatus(
            AppUpdateStatus.Failed,
            error = message
        )

        return false
    }

    private fun sendStatus(
        status: AppUpdateStatus,
        version: String? = null,
        progress: Int? = null,
        message: String? = null,
        error: String? = null
    ) {
        sseManager.send(
            SSEAppUpdater(
                AppUpdateProgress(
                    status = status,
                    version = version,
                    progress = progress,
                    message = message,
                    error = error
                )
            )
        )
    }
}