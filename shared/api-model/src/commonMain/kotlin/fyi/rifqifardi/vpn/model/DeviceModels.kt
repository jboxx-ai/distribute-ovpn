package fyi.rifqifardi.vpn.model

import kotlinx.serialization.Serializable

@Serializable
enum class DevicePlatform {
    ANDROID,
    MACOS,
    LINUX,
    WINDOWS,
    UNKNOWN
}

@Serializable
enum class DeviceStatus {
    ACTIVE,
    REVOKED,
    SUSPENDED
}

@Serializable
data class EnrollmentTokenRequest(
    val deviceName: String,
    val platform: DevicePlatform,
    val assignedProfileId: String,
    val assignedChannel: ReleaseChannel = ReleaseChannel.STABLE,
    val expiresInMinutes: Long = 60
)

@Serializable
data class EnrollmentTokenResponse(
    val token: String,
    val expiresAtEpochMs: Long
)

@Serializable
data class EnrollDeviceRequest(
    val token: String,
    val devicePublicKeyBase64: String? = null
)

@Serializable
data class EnrolledDeviceCredentials(
    val deviceId: String,
    val apiKey: String,
    val assignedProfileId: String,
    val assignedChannel: ReleaseChannel,
    val signingPublicKeyBase64: String
)

@Serializable
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val platform: DevicePlatform,
    val status: DeviceStatus,
    val assignedProfileId: String,
    val assignedChannel: ReleaseChannel,
    val enrolledAtEpochMs: Long,
    val lastSeenAtEpochMs: Long?,
    val userId: String? = null,
    val tokenExpiresAtEpochMs: Long? = null
)

@Serializable
data class ClientAppRelease(
    val id: String,
    val platform: DevicePlatform,
    val version: String,
    val channel: ReleaseChannel = ReleaseChannel.STABLE,
    val fileName: String,
    val downloadUrl: String,
    val fileSizeBytes: Long,
    val sha256Checksum: String,
    val minOsVersion: String,
    val releaseNotes: String? = null,
    val publishedAtEpochMs: Long
)

@Serializable
data class PublishClientAppRequest(
    val platform: DevicePlatform,
    val version: String,
    val channel: ReleaseChannel = ReleaseChannel.STABLE,
    val fileName: String,
    val downloadUrl: String,
    val fileSizeBytes: Long,
    val sha256Checksum: String,
    val minOsVersion: String,
    val releaseNotes: String? = null
)

