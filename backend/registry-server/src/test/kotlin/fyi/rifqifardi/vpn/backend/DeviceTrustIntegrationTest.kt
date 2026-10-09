package fyi.rifqifardi.vpn.backend

import fyi.rifqifardi.vpn.backend.auth.TotpService
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
                ProfilesTable,
                UsersTable
            )
            SchemaUtils.create(
                UsersTable,
                ProfilesTable,
                ProfileVersionsTable,
                ChannelPublicationsTable,
                DevicesTable,
                EnrollmentTokensTable
            )
        }
    }

    @Test
    fun testUserTotpRegistrationLoginAndDeviceTrust() = testApplication {
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

        // 2. Register Operator / User
        val regResp = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterUserRequest(username = "rifqi", password = "SecurePassword123!", role = UserRole.ADMIN))
        }
        assertEquals(HttpStatusCode.Created, regResp.status)

        // 3. Retrieve TOTP Setup
        val totpSetupResp = client.get("/api/v1/auth/totp/setup?username=rifqi")
        assertEquals(HttpStatusCode.OK, totpSetupResp.status)
        val totpSetup = totpSetupResp.body<ApiEnvelope<TotpSetupResponse>>().data!!
        val userSecret = totpSetup.secret

        // 4. Login with Password + TOTP Code
        val currentTotp = TotpService.generateCurrentCode(userSecret)
        val loginResp = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "rifqi", password = "SecurePassword123!", totpCode = currentTotp))
        }
        assertEquals(HttpStatusCode.OK, loginResp.status)
        val loginEnvelope = loginResp.body<ApiEnvelope<LoginResponse>>()
        assertTrue(loginEnvelope.success)
        assertEquals("rifqi", loginEnvelope.data!!.user.username)
        assertEquals(UserRole.ADMIN, loginEnvelope.data!!.user.role)

        // 5. Authorize Device using In-App Native TOTP Flow (Phase 11 Primary Flow)
        val authDeviceTotp = TotpService.generateCurrentCode(userSecret)
        val deviceAuthResp = client.post("/api/v1/devices/authorize") {
            contentType(ContentType.Application.Json)
            setBody(
                AuthorizeDeviceRequest(
                    username = "rifqi",
                    password = "SecurePassword123!",
                    totpCode = authDeviceTotp,
                    deviceName = "Rifqi-MacBook-M3",
                    platform = DevicePlatform.MACOS,
                    devicePublicKeyBase64 = "MCowBQYDK2VwAyEAGb9ECW4pG/Zq5G006y6B5xK1N62H9R==",
                    assignedProfileId = "prod-vpn",
                    assignedChannel = ReleaseChannel.STABLE
                )
            )
        }
        assertEquals(HttpStatusCode.Created, deviceAuthResp.status)
        val deviceAuthEnvelope = deviceAuthResp.body<ApiEnvelope<DeviceAuthorizationResponse>>()
        assertTrue(deviceAuthEnvelope.success)
        val devAuth = deviceAuthEnvelope.data!!
        val deviceToken = devAuth.deviceToken
        val deviceId = devAuth.deviceId

        // 6. Policy A: Perform Authenticated Pre-Flight Sync with Issued Device Token
        val manifestResp = client.get("/api/v1/sync/manifest") {
            header("Authorization", "Bearer $deviceToken")
        }
        assertEquals(HttpStatusCode.OK, manifestResp.status)
        val manifestEnvelope = manifestResp.body<ApiEnvelope<ProfileManifest>>()
        assertTrue(manifestEnvelope.success)
        val manifest = manifestEnvelope.data!!

        // 7. Verify Ed25519 Payload Signature
        val isSigValid = signatureVerifier.verify(
            ovpn.toByteArray(Charsets.UTF_8),
            manifest.signature,
            devAuth.signingPublicKeyBase64
        )
        assertTrue(isSigValid, "Downloaded payload signature must verify against server public key")

        // 8. Remote Revocation: Revoke Device from Console
        val revokeResp = client.post("/api/v1/devices/$deviceId/revoke")
        assertEquals(HttpStatusCode.OK, revokeResp.status)

        // 9. Policy A Enforcement: Immediate 403 Forbidden after Revocation
        val revokedSyncResp = client.get("/api/v1/sync/manifest") {
            header("Authorization", "Bearer $deviceToken")
        }
        assertEquals(HttpStatusCode.Forbidden, revokedSyncResp.status)
    }
}
