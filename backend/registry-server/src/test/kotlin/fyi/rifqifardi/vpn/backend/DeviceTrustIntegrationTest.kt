package fyi.rifqifardi.vpn.backend

import fyi.rifqifardi.vpn.backend.db.*
import fyi.rifqifardi.vpn.backend.service.ProfileRegistryService
import fyi.rifqifardi.vpn.crypto.Ed25519SignatureVerifier
import fyi.rifqifardi.vpn.model.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeviceTrustIntegrationTest {

    private val service = ProfileRegistryService()
    private val signatureVerifier = Ed25519SignatureVerifier()

    @BeforeTest
    fun setupDb() {
        Database.connect("jdbc:h2:mem:device_test_db;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.drop(
                EnrollmentTokensTable,
                DevicesTable,
                ChannelPublicationsTable,
                ProfileVersionsTable,
                ProfilesTable
            )
            SchemaUtils.create(
                ProfilesTable,
                ProfileVersionsTable,
                ChannelPublicationsTable,
                DevicesTable,
                EnrollmentTokensTable
            )
        }
    }

    @Test
    fun testDeviceEnrollmentAndAuthenticatedSync() = testApplication {
        application {
            module(service)
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        // 1. Create Profile & Upload Version
        service.createProfile("prod-vpn", "Production VPN", "Production network")
        val ovpn = """
            client
            dev tun
            proto udp
            remote vpn.corp.internal 1194
            <ca>
            test-ca
            </ca>
        """.trimIndent()
        service.uploadVersion("prod-vpn", ovpn)
        service.publishToChannel("prod-vpn", 1, ReleaseChannel.STABLE)

        // 2. Generate Enrollment Token for Device
        val tokenResp = client.post("/api/v1/devices/tokens") {
            contentType(ContentType.Application.Json)
            setBody(
                EnrollmentTokenRequest(
                    deviceName = "Rifqi-Pixel-9",
                    platform = DevicePlatform.ANDROID,
                    assignedProfileId = "prod-vpn",
                    assignedChannel = ReleaseChannel.STABLE
                )
            )
        }
        assertEquals(HttpStatusCode.Created, tokenResp.status)
        val tokenEnvelope = tokenResp.body<ApiEnvelope<EnrollmentTokenResponse>>()
        assertTrue(tokenEnvelope.success)
        val token = tokenEnvelope.data!!.token

        // 3. Enroll Device using Token
        val enrollResp = client.post("/api/v1/devices/enroll") {
            contentType(ContentType.Application.Json)
            setBody(EnrollDeviceRequest(token = token))
        }
        assertEquals(HttpStatusCode.Created, enrollResp.status)
        val credsEnvelope = enrollResp.body<ApiEnvelope<EnrolledDeviceCredentials>>()
        assertTrue(credsEnvelope.success)
        val creds = credsEnvelope.data!!
        assertEquals("prod-vpn", creds.assignedProfileId)
        assertEquals(ReleaseChannel.STABLE, creds.assignedChannel)

        // 4. Token cannot be reused
        val reuseResp = client.post("/api/v1/devices/enroll") {
            contentType(ContentType.Application.Json)
            setBody(EnrollDeviceRequest(token = token))
        }
        assertEquals(HttpStatusCode.Unauthorized, reuseResp.status)

        // 5. Authenticated Sync Manifest Call
        val manifestResp = client.get("/api/v1/sync/manifest") {
            header("Authorization", "Bearer ${creds.apiKey}")
        }
        assertEquals(HttpStatusCode.OK, manifestResp.status)
        val manifestEnvelope = manifestResp.body<ApiEnvelope<ProfileManifest>>()
        assertTrue(manifestEnvelope.success)
        val manifest = manifestEnvelope.data!!

        // 6. Verify Cryptographic Signature on Manifest / Payload
        val isSigValid = signatureVerifier.verify(
            ovpn.toByteArray(Charsets.UTF_8),
            manifest.signature,
            creds.signingPublicKeyBase64
        )
        assertTrue(isSigValid, "Device must successfully verify server's Ed25519 signature on profile bytes")

        // 7. Revoke Device and verify access is immediately blocked
        val revokeResp = client.post("/api/v1/devices/${creds.deviceId}/revoke")
        assertEquals(HttpStatusCode.OK, revokeResp.status)

        val blockedSyncResp = client.get("/api/v1/sync/manifest") {
            header("Authorization", "Bearer ${creds.apiKey}")
        }
        assertEquals(HttpStatusCode.Forbidden, blockedSyncResp.status)
    }
}
