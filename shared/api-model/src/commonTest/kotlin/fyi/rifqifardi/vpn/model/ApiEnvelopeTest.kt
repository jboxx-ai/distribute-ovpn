package fyi.rifqifardi.vpn.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiEnvelopeTest {
    private val json = Json { prettyPrint = true }

    @Test
    fun testEnvelopeSerialization() {
        val manifest = ProfileManifest(
            schemaVersion = 1,
            profileId = "prof-work",
            version = 42,
            channel = ReleaseChannel.STABLE,
            digest = ProfileDigest(algorithm = "SHA-256", hexValue = "abcdef0123456789"),
            signature = ProfileSignature(algorithm = "Ed25519", keyId = "key-1", signatureBase64 = "base64sig"),
            downloadUrl = "https://vpn.corp.internal/profiles/prof-work/v42.ovpn",
            createdAtEpochMs = 1728300000000L
        )

        val envelope = ApiEnvelope.ok(manifest)
        val serialized = json.encodeToString(ApiEnvelope.serializer(ProfileManifest.serializer()), envelope)
        val deserialized = json.decodeFromString(ApiEnvelope.serializer(ProfileManifest.serializer()), serialized)

        assertTrue(deserialized.success)
        assertEquals(42L, deserialized.data?.version)
        assertEquals(ReleaseChannel.STABLE, deserialized.data?.channel)
    }

    @Test
    fun testFailureEnvelope() {
        val failure = ApiEnvelope.failure<ProfileManifest>(ErrorCode.DEVICE_REVOKED, "Device revoked by admin")
        val serialized = json.encodeToString(ApiEnvelope.serializer(ProfileManifest.serializer()), failure)
        val deserialized = json.decodeFromString(ApiEnvelope.serializer(ProfileManifest.serializer()), serialized)

        assertEquals(false, deserialized.success)
        assertEquals(ErrorCode.DEVICE_REVOKED, deserialized.error?.code)
    }
}
