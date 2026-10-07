package fyi.rifqifardi.vpn.backend

import fyi.rifqifardi.vpn.backend.db.ChannelPublicationsTable
import fyi.rifqifardi.vpn.backend.db.ProfileVersionsTable
import fyi.rifqifardi.vpn.backend.db.ProfilesTable
import fyi.rifqifardi.vpn.backend.service.ProfileRegistryService
import fyi.rifqifardi.vpn.model.ApiEnvelope
import fyi.rifqifardi.vpn.model.ProfileManifest
import fyi.rifqifardi.vpn.model.ReleaseChannel
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

class RegistryApiIntegrationTest {

    @BeforeTest
    fun setupDb() {
        Database.connect("jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.drop(ChannelPublicationsTable, ProfileVersionsTable, ProfilesTable)
            SchemaUtils.create(ProfilesTable, ProfileVersionsTable, ChannelPublicationsTable)
        }
    }

    @Test
    fun testFullProfileLifecycle() = testApplication {
        application {
            module(ProfileRegistryService())
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        // 1. Create Profile
        val createResp = client.post("/api/v1/profiles") {
            contentType(ContentType.Application.Json)
            setBody(CreateProfileRequest(id = "corp-vpn", name = "Corporate VPN", description = "Main corporate gateway"))
        }
        assertEquals(HttpStatusCode.Created, createResp.status)

        // 2. Upload valid version
        val ovpn = """
            client
            dev tun
            proto udp
            remote vpn.corp.internal 1194
            <ca>
            cert-data
            </ca>
        """.trimIndent()

        val uploadResp = client.post("/api/v1/profiles/corp-vpn/versions") {
            contentType(ContentType.Application.Json)
            setBody(UploadVersionRequest(ovpnContent = ovpn, signature = "sig_v1", keyId = "key_1"))
        }
        assertEquals(HttpStatusCode.Created, uploadResp.status)

        // 3. Promote to STABLE
        val promoteResp = client.post("/api/v1/profiles/corp-vpn/channels") {
            contentType(ContentType.Application.Json)
            setBody(PromoteChannelRequest(version = 1, channel = ReleaseChannel.STABLE))
        }
        assertEquals(HttpStatusCode.OK, promoteResp.status)

        // 4. Fetch manifest
        val manifestResp = client.get("/api/v1/profiles/corp-vpn/manifest?channel=STABLE")
        assertEquals(HttpStatusCode.OK, manifestResp.status)
        val envelope = manifestResp.body<ApiEnvelope<ProfileManifest>>()
        assertTrue(envelope.success)
        assertEquals(1L, envelope.data?.version)
        assertEquals(ReleaseChannel.STABLE, envelope.data?.channel)

        // 5. Fetch artifact
        val artifactResp = client.get("/api/v1/profiles/corp-vpn/versions/1/artifact")
        assertEquals(HttpStatusCode.OK, artifactResp.status)
        assertEquals(ovpn, artifactResp.body())
    }
}
