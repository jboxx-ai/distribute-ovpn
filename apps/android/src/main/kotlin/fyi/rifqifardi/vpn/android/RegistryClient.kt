package fyi.rifqifardi.vpn.android

import fyi.rifqifardi.vpn.model.ApiEnvelope
import fyi.rifqifardi.vpn.model.EnrollDeviceRequest
import fyi.rifqifardi.vpn.model.EnrolledDeviceCredentials
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.HttpTimeout

internal class RegistryClient(private val http: HttpClient = HttpClient {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 15_000
    }
}) {
    suspend fun checkHealth(baseUrl: String): Result<Unit> = runCatching {
        val response = http.get("${normalizeBaseUrl(baseUrl)}/healthz")
        check(response.status.value in 200..299) { "Registry health check failed (${response.status.value})" }
        val body = response.body<HealthResponse>()
        check(body.status == "UP") { "Registry is not healthy" }
    }

    suspend fun enroll(baseUrl: String, token: String): Result<EnrolledDeviceCredentials> = runCatching {
        require(token.isNotBlank()) { "Enrollment token is required" }
        val response = http.post("${normalizeBaseUrl(baseUrl)}/api/v1/devices/enroll") {
            contentType(ContentType.Application.Json)
            setBody(EnrollDeviceRequest(token = token))
        }
        check(response.status.value in 200..299) { "Enrollment failed (${response.status.value})" }
        val envelope = response.body<ApiEnvelope<EnrolledDeviceCredentials>>()
        check(envelope.success) { "Enrollment was rejected" }
        envelope.data ?: error("Registry returned no enrollment credentials")
    }

    private fun normalizeBaseUrl(raw: String): String {
        val value = raw.trim().trimEnd('/')
        val isTrustedTailscaleHttp = value.startsWith("http://100.") &&
            value.removePrefix("http://").substringBefore('/').substringBefore(':')
                .split('.').firstOrNull()?.toIntOrNull() == 100
        require(value.startsWith("https://", ignoreCase = true) || isTrustedTailscaleHttp) {
            "Use HTTPS, or trusted Tailscale HTTP (100.64.0.0/10), for the registry address"
        }
        require('?' !in value && '#' !in value && value.length <= 2048) { "Invalid registry address" }
        return value
    }

    @Serializable
    private data class HealthResponse(val status: String)
}
