package no.iktdev.kammich.utils

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Component
class RestClientFactory(
) {
    // Standard klient for vanlig REST (CRUD, post, get)
    fun create(baseUrl: String): RestClient {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", "application/json")
            .build()
    }

    // Spesialklient for SSE-strømmer basert på Java sin innebygde HttpClient
    fun createSseClient(baseUrl: String): SseClientWrapper {
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

        return SseClientWrapper(baseUrl, httpClient)
    }
}

// Hjelpeklasse for å håndtere SSE-strømmer synkront/reaktivt uten WebFlux
class SseClientWrapper(
    private val baseUrl: String,
    private val httpClient: HttpClient
) {
    fun stream(path: String, onMessage: (String) -> Unit) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Accept", "text/event-stream")
            .GET()
            .build()

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
            .thenAccept { response ->
                response.body().forEach { line ->
                    onMessage(line)
                }
            }
    }
}