package no.iktdev.kammich.services

import no.iktdev.kammich.models.shared.Version
import no.iktdev.kammich.storage.internal.DiskHealthService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class VersionService(
    private val gitHubReleaseService: GitHubReleaseService
) {
    private val log = LoggerFactory.getLogger(DiskHealthService::class.java)

    fun getVersion(): Version {
        val current = getCurrentVersion()
        val release = getLatestRelease()

        return Version(
            kammichVersion = current,
            kammichGithubVersion = release
                ?.tagName
                ?.removePrefix("v")
                ?: "Unknown",
            updateAvailable = release?.let {
                isNewerVersion(current, it.tagName)
            } ?: false,
            updatable = isUpdatable()
        )
    }

    fun getCurrentVersion(): String =
        VersionService::class.java.`package`.implementationVersion
            ?: "Unknown"

    fun getLatestRelease(): GitHubReleaseService.GitHubRelease? =
        gitHubReleaseService.getLatestRelease()

    fun isUpdateAvailable(): Boolean {
        val release = getLatestRelease() ?: return false

        return isNewerVersion(
            getCurrentVersion(),
            release.tagName
        )
    }

    fun isUpdatable(): Boolean =
        getRunningJar() != null

    fun getRunningJar(): File? {
        val location = UpdateService::class.java
            .protectionDomain
            .codeSource
            .location
            .toExternalForm()

        return location
            .takeIf { it.startsWith("jar:nested:") }
            ?.removePrefix("jar:nested:")
            ?.substringBefore("/!")
            ?.let(::File)
            ?.takeIf { it.isFile }
    }

    fun isNewerVersion(
        current: String,
        latest: String
    ): Boolean {
        val currentVersion = parseVersion(current) ?: return false
        val latestVersion = parseVersion(latest) ?: return false

        return latestVersion > currentVersion
    }

    private fun parseVersion(version: String): VersionNumber? {
        val parts = version
            .removePrefix("v")
            .substringBefore("-")
            .split(".")

        if (parts.size != 3) return null

        return VersionNumber(
            major = parts[0].toIntOrNull() ?: return null,
            minor = parts[1].toIntOrNull() ?: return null,
            patch = parts[2].toIntOrNull() ?: return null
        )
    }

    private data class VersionNumber(
        val major: Int,
        val minor: Int,
        val patch: Int
    ) : Comparable<VersionNumber> {

        override fun compareTo(other: VersionNumber): Int =
            compareValuesBy(
                this,
                other,
                VersionNumber::major,
                VersionNumber::minor,
                VersionNumber::patch
            )
    }
}