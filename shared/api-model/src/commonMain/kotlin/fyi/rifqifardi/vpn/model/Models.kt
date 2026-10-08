package fyi.rifqifardi.vpn.model

import kotlinx.serialization.Serializable

@Serializable
enum class ErrorCode {
    INVALID_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    DEVICE_REVOKED,
    INTERNAL_ERROR
}

@Serializable
data class ApiEnvelope<T>(
    val schemaVersion: Int = 1,
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null
) {
    companion object {
        fun <T> ok(data: T, schemaVersion: Int = 1): ApiEnvelope<T> =
            ApiEnvelope(schemaVersion = schemaVersion, success = true, data = data, error = null)

        fun <T> failure(code: ErrorCode, message: String, schemaVersion: Int = 1): ApiEnvelope<T> =
            ApiEnvelope(
                schemaVersion = schemaVersion,
                success = false,
                data = null,
                error = ApiError(code = code, message = message)
            )
    }
}

@Serializable
data class ApiError(
    val code: ErrorCode,
    val message: String,
    val details: Map<String, String>? = null
)

@Serializable
enum class ReleaseChannel {
    STABLE,
    BETA,
    CANARY
}

@Serializable
enum class TunnelState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTING,
    FAILED
}

@Serializable
data class ProfileDigest(
    val algorithm: String = "SHA-256",
    val hexValue: String
)

@Serializable
data class ProfileSignature(
    val algorithm: String = "Ed25519",
    val keyId: String,
    val signatureBase64: String
)

@Serializable
data class ProfileManifest(
    val schemaVersion: Int = 1,
    val profileId: String,
    val version: Long,
    val channel: ReleaseChannel,
    val digest: ProfileDigest,
    val signature: ProfileSignature,
    val downloadUrl: String,
    val createdAtEpochMs: Long
)

@Serializable
data class ProfileSummary(
    val id: String,
    val name: String,
    val description: String? = null,
    val createdAtEpochMs: Long,
    val totalVersions: Long,
    val latestVersion: Long,
    val stableVersion: Long? = null,
    val betaVersion: Long? = null,
    val canaryVersion: Long? = null
)

@Serializable
data class AuditLogEntry(
    val id: String,
    val actor: String,
    val action: String,
    val targetType: String,
    val targetId: String,
    val detailsSanitized: String,
    val timestampEpochMs: Long
)
