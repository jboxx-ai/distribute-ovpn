package fyi.rifqifardi.vpn.sync.auth

import fyi.rifqifardi.vpn.domain.contracts.SecureStorage
import fyi.rifqifardi.vpn.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

interface AuthRepository {
    suspend fun authorizeDevice(baseUrl: String, req: AuthorizeDeviceRequest): Result<DeviceAuthorizationResponse>
    suspend fun isEnrolled(): Boolean
    suspend fun getDeviceToken(): String?
    suspend fun getEnrolledUserId(): String?
    suspend fun getTokenExpirationEpochMs(): Long?
    suspend fun logout()
}

class ClientAuthEngine(
    private val secureStorage: SecureStorage,
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }
) : AuthRepository {

    override suspend fun authorizeDevice(
        baseUrl: String,
        req: AuthorizeDeviceRequest
    ): Result<DeviceAuthorizationResponse> {
        return try {
            val resp = httpClient.post("$baseUrl/api/v1/devices/authorize") {
                contentType(ContentType.Application.Json)
                setBody(req)
            }

            if (!resp.status.isSuccess()) {
                val errMessage = try {
                    val envelope = resp.body<ApiEnvelope<Unit>>()
                    envelope.error?.message ?: "Authorization failed: ${resp.status}"
                } catch (e: Exception) {
                    "Authorization failed with status ${resp.status}"
                }
                return Result.failure(IllegalStateException(errMessage))
            }

            val envelope = resp.body<ApiEnvelope<DeviceAuthorizationResponse>>()
            val data = envelope.data ?: return Result.failure(IllegalStateException("No authorization data in response"))

            // Persist securely in client storage
            secureStorage.setString("device_id", data.deviceId)
            secureStorage.setString("device_token", data.deviceToken)
            secureStorage.setString("device_api_key", data.deviceToken) // Backwards-compatible
            secureStorage.setString("user_id", data.userId)
            secureStorage.setString("assigned_profile_id", data.assignedProfileId)
            secureStorage.setString("assigned_channel", data.assignedChannel.name)
            secureStorage.setString("server_signing_public_key", data.signingPublicKeyBase64)
            secureStorage.setString("token_expires_at", data.expiresAtEpochMs.toString())

            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isEnrolled(): Boolean {
        val token = secureStorage.getString("device_token") ?: secureStorage.getString("device_api_key")
        if (token.isNullOrEmpty()) return false

        val expiresAt = secureStorage.getString("token_expires_at")?.toLongOrNull()
        if (expiresAt != null && kotlinx.datetime.Clock.System.now().toEpochMilliseconds() > expiresAt) {
            return false
        }
        return true
    }

    override suspend fun getDeviceToken(): String? {
        return secureStorage.getString("device_token") ?: secureStorage.getString("device_api_key")
    }

    override suspend fun getEnrolledUserId(): String? {
        return secureStorage.getString("user_id")
    }

    override suspend fun getTokenExpirationEpochMs(): Long? {
        return secureStorage.getString("token_expires_at")?.toLongOrNull()
    }

    override suspend fun logout() {
        secureStorage.remove("device_token")
        secureStorage.remove("device_api_key")
        secureStorage.remove("device_id")
        secureStorage.remove("user_id")
        secureStorage.remove("token_expires_at")
        secureStorage.remove("active_version")
        secureStorage.remove("staged_version")
    }
}
