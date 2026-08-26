package no.iktdev.kammich.controller;

import no.iktdev.kammich.models.shared.Version
import no.iktdev.kammich.services.GitHubReleaseService
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/version")
class VersionController(
    private val gitHubReleaseService: GitHubReleaseService
) {

    @GetMapping
    fun getVersion(): Version {
        val current = this::class.java.`package`.implementationVersion ?: "Unknown"
        val latest = gitHubReleaseService
            .getLatestRelease()
            ?.tagName
            ?.removePrefix("v")
            ?: "Unknown"

        return Version(
            kammichVersion = current,
            kammichGithubVersion = latest,
            updateAvailable = isNewerVersion(latest, current)
        )
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestVersion = parseVersion(latest) ?: return false
        val currentVersion = parseVersion(current) ?: return false

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