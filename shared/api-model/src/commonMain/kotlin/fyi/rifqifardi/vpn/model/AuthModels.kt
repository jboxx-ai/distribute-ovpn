package fyi.rifqifardi.vpn.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    ADMIN,
    OPERATOR,
    AUDITOR
}

@Serializable
enum class UserStatus {
    ACTIVE,
    DISABLED
}

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val role: UserRole,
    val status: UserStatus
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val totpCode: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserProfile,
    val expiresAtEpochMs: Long
)

@Serializable
data class RegisterUserRequest(
    val username: String,
    val password: String,
    val role: UserRole = UserRole.OPERATOR
)

@Serializable
data class TotpSetupResponse(
    val username: String,
    val secret: String,
    val qrCodeUri: String
)

@Serializable
data class AuthorizeDeviceRequest(
    val username: String,
    val password: String,
    val totpCode: String,
    val deviceName: String,
    val platform: DevicePlatform,
    val devicePublicKeyBase64: String? = null,
    val assignedProfileId: String,
    val assignedChannel: ReleaseChannel = ReleaseChannel.STABLE,
    val tokenValidityDays: Long = 30
)

@Serializable
data class DeviceAuthorizationResponse(
    val deviceId: String,
    val deviceToken: String,
    val userId: String,
    val assignedProfileId: String,
    val assignedChannel: ReleaseChannel,
    val signingPublicKeyBase64: String,
    val expiresAtEpochMs: Long
)
