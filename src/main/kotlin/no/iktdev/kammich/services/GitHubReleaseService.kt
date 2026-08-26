package no.iktdev.kammich.services

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class GitHubReleaseService {

    private val client = RestClient.builder()
        .baseUrl("https://api.github.com")
        .defaultHeader("Accept", "application/vnd.github+json")
        .defaultHeader("X-GitHub-Api-Version", "2026-03-10")
        .build()

    fun getLatestRelease(): GitHubRelease? {
        return client.get()
            .uri("/repos/iktdev-no/Kammich/releases/latest")
            .retrieve()
            .body(GitHubRelease::class.java)
    }

    data class GitHubRelease(
        @JsonProperty("tag_name")
        val tagName: String,
        val name: String,
        val prerelease: Boolean,
        val draft: Boolean
    )
}