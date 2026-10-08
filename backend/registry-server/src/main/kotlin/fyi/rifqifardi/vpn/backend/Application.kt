package fyi.rifqifardi.vpn.backend

import fyi.rifqifardi.vpn.backend.audit.AuditLogsTable
import fyi.rifqifardi.vpn.backend.db.*
import fyi.rifqifardi.vpn.backend.service.ProfileRegistryService
import fyi.rifqifardi.vpn.model.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class CreateProfileRequest(
    val id: String,
    val name: String,
    val description: String? = null
)

@Serializable
data class UploadVersionRequest(
    val ovpnContent: String
)

@Serializable
data class PromoteChannelRequest(
    val version: Long,
    val channel: ReleaseChannel
)

fun Application.module(service: ProfileRegistryService = ProfileRegistryService()) {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiEnvelope.failure<Unit>(ErrorCode.INTERNAL_ERROR, cause.message ?: "Unknown error")
            )
        }
    }

    routing {
        // --- Static UI Console & Assets Routing ---
        val consoleDist = File("backend/console-ui/dist")
        val assetsDist = File("backend/console-ui/dist/assets")
        val prototypesDir = File("docs/prototypes")

        if (assetsDist.exists()) {
            staticFiles("/assets", assetsDist)
        }

        if (consoleDist.exists()) {
            staticFiles("/console", consoleDist) {
                default("index.html")
            }
        }

        if (prototypesDir.exists()) {
            staticFiles("/prototypes", prototypesDir) {
                default("index.html")
            }
        }

        // --- Root Welcome & Documentation ---
        get("/") {
            val distFile = File("backend/console-ui/dist/index.html")
            if (distFile.exists()) {
                call.respondFile(distFile)
            } else {
                val html = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <title>OpenSpec KMP VPN Registry Server</title>
                        <style>
                            body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; max-width: 800px; margin: 40px auto; padding: 0 20px; line-height: 1.6; color: #e6edf3; background: #0d1117; }
                            h1 { border-bottom: 1px solid #30363d; padding-bottom: 8px; }
                            .badge { display: inline-block; padding: 4px 8px; border-radius: 6px; font-size: 12px; font-weight: 600; background: #1f3fb950; color: #3fb950; border: 1px solid #3fb950; }
                            pre { background: #010409; padding: 16px; border-radius: 6px; overflow-x: auto; border: 1px solid #30363d; }
                            code { font-family: ui-monospace, monospace; font-size: 13px; color: #f38020; }
                            a { color: #58a6ff; text-decoration: none; }
                            a:hover { text-decoration: underline; }
                        </style>
                    </head>
                    <body>
                        <h1>OpenSpec KMP VPN Registry <span class="badge">ONLINE</span></h1>
                        <p>Welcome to the self-hosted configuration control plane for the OpenSpec KMP VPN Platform (Cloudflare Kumo UI).</p>
                        
                        <h3>Surfaces & Console</h3>
                        <ul>
                            <li><a href="/console"><code>/console</code></a> — Cloudflare Kumo Web Console SPA</li>
                            <li><a href="/prototypes/index.html"><code>/prototypes/index.html</code></a> — Operator Console Prototype</li>
                            <li><a href="/prototypes/android-client.html"><code>/prototypes/android-client.html</code></a> — Android Client Prototype</li>
                            <li><a href="/prototypes/macos-client.html"><code>/prototypes/macos-client.html</code></a> — macOS Client Prototype</li>
                        </ul>

                        <h3>System Endpoints</h3>
                        <ul>
                            <li><a href="/healthz"><code>GET /healthz</code></a> — Liveness & database probe</li>
                            <li><code>POST /api/v1/devices/tokens</code> — Generate single-use enrollment token</li>
                            <li><code>POST /api/v1/devices/enroll</code> — Device enrollment & credential issuance</li>
                            <li><code>GET /api/v1/sync/manifest</code> — Authenticated profile sync</li>
                        </ul>
                    </body>
                    </html>
                """.trimIndent()
                call.respondText(html, ContentType.Text.Html)
            }
        }

        // --- Healthcheck Probe ---
        get("/healthz") {
            val dbHealthy = try {
                transaction {
                    ProfilesTable.selectAll().limit(1).count()
                }
                true
            } catch (e: Exception) {
                false
            }

            if (dbHealthy) {
                call.respond(HttpStatusCode.OK, mapOf("status" to "UP", "database" to "CONNECTED", "ui" to "KUMO_ACTIVE"))
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "DOWN", "database" to "DISCONNECTED"))
            }
        }

        // --- Public / Device Enrollment API ---
        route("/api/v1/devices") {
            post("/tokens") {
                val req = call.receive<EnrollmentTokenRequest>()
                val res = service.createEnrollmentToken(req)
                call.respond(HttpStatusCode.Created, ApiEnvelope.ok(res))
            }

            post("/enroll") {
                val req = call.receive<EnrollDeviceRequest>()
                val creds = service.enrollDevice(req)
                if (creds != null) {
                    call.respond(HttpStatusCode.Created, ApiEnvelope.ok(creds))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, ApiEnvelope.failure<Unit>(ErrorCode.UNAUTHORIZED, "Invalid or expired enrollment token"))
                }
            }

            post("/{id}/revoke") {
                val deviceId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val revoked = service.revokeDevice(deviceId)
                if (revoked) {
                    call.respond(HttpStatusCode.OK, ApiEnvelope.ok(mapOf("revoked" to true)))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiEnvelope.failure<Unit>(ErrorCode.NOT_FOUND, "Device not found"))
                }
            }
        }

        // --- Authenticated Device Profile Sync API ---
        route("/api/v1/sync") {
            get("/manifest") {
                val authHeader = call.request.headers["Authorization"]
                val apiKey = authHeader?.removePrefix("Bearer ")?.trim()
                if (apiKey.isNullOrEmpty()) {
                    return@get call.respond(HttpStatusCode.Unauthorized, ApiEnvelope.failure<Unit>(ErrorCode.UNAUTHORIZED, "Missing Bearer token"))
                }

                val device = service.authenticateDevice(apiKey)
                if (device == null) {
                    return@get call.respond(HttpStatusCode.Forbidden, ApiEnvelope.failure<Unit>(ErrorCode.FORBIDDEN, "Device revoked or unauthorized"))
                }

                val baseUrl = "${call.request.local.scheme}://${call.request.local.serverHost}:${call.request.local.serverPort}"
                val manifest = service.getManifest(device.assignedProfileId, device.assignedChannel, baseUrl)
                if (manifest != null) {
                    call.respond(HttpStatusCode.OK, ApiEnvelope.ok(manifest))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiEnvelope.failure<Unit>(ErrorCode.NOT_FOUND, "No active profile version published for device channel"))
                }
            }
        }

        // --- Admin Profile & Version Management API ---
        route("/api/v1/profiles") {
            post {
                val req = call.receive<CreateProfileRequest>()
                val created = service.createProfile(req.id, req.name, req.description)
                if (created) {
                    call.respond(HttpStatusCode.Created, ApiEnvelope.ok(mapOf("id" to req.id)))
                } else {
                    call.respond(HttpStatusCode.Conflict, ApiEnvelope.failure<Unit>(ErrorCode.CONFLICT, "Profile already exists"))
                }
            }

            post("/{id}/versions") {
                val profileId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val req = call.receive<UploadVersionRequest>()
                val result = service.uploadVersion(profileId, req.ovpnContent)
                result.fold(
                    onSuccess = { ver ->
                        call.respond(HttpStatusCode.Created, ApiEnvelope.ok(mapOf("version" to ver)))
                    },
                    onFailure = { err ->
                        call.respond(HttpStatusCode.BadRequest, ApiEnvelope.failure<Unit>(ErrorCode.INVALID_REQUEST, err.message ?: "Invalid profile"))
                    }
                )
            }

            post("/{id}/channels") {
                val profileId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val req = call.receive<PromoteChannelRequest>()
                val success = service.publishToChannel(profileId, req.version, req.channel)
                if (success) {
                    call.respond(HttpStatusCode.OK, ApiEnvelope.ok(mapOf("promoted" to true)))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiEnvelope.failure<Unit>(ErrorCode.NOT_FOUND, "Profile version not found"))
                }
            }

            get("/{id}/manifest") {
                val profileId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val channelParam = call.request.queryParameters["channel"] ?: "STABLE"
                val channel = try {
                    ReleaseChannel.valueOf(channelParam.uppercase())
                } catch (e: Exception) {
                    ReleaseChannel.STABLE
                }

                val baseUrl = "${call.request.local.scheme}://${call.request.local.serverHost}:${call.request.local.serverPort}"
                val manifest = service.getManifest(profileId, channel, baseUrl)
                if (manifest != null) {
                    call.respond(HttpStatusCode.OK, ApiEnvelope.ok(manifest))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiEnvelope.failure<Unit>(ErrorCode.NOT_FOUND, "No manifest found for channel $channel"))
                }
            }

            get("/{id}/versions/{version}/artifact") {
                val profileId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val ver = call.parameters["version"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val artifact = service.getArtifact(profileId, ver)
                if (artifact != null) {
                    call.respondText(artifact, ContentType.Text.Plain)
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiEnvelope.failure<Unit>(ErrorCode.NOT_FOUND, "Artifact version not found"))
                }
            }
        }
    }
}

fun main() {
    Database.connect(
        url = System.getenv("JDBC_DATABASE_URL") ?: "jdbc:h2:mem:vpn_registry;DB_CLOSE_DELAY=-1",
        driver = System.getenv("JDBC_DRIVER") ?: "org.h2.Driver",
        user = System.getenv("DB_USER") ?: "sa",
        password = System.getenv("DB_PASSWORD") ?: ""
    )

    transaction {
        SchemaUtils.create(
            ProfilesTable,
            ProfileVersionsTable,
            ChannelPublicationsTable,
            DevicesTable,
            EnrollmentTokensTable,
            AuditLogsTable
        )
    }

    embeddedServer(Netty, port = System.getenv("PORT")?.toIntOrNull() ?: 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}
