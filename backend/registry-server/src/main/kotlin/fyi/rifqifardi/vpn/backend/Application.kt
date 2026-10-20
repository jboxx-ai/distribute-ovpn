package fyi.rifqifardi.vpn.backend

import fyi.rifqifardi.vpn.backend.audit.AuditLogsTable
import fyi.rifqifardi.vpn.backend.auth.PasswordService
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
import org.jetbrains.exposed.sql.insert
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
        val rootDir = File(System.getenv("VPN_APP_ROOT") ?: System.getProperty("user.dir"))
        fun resolveFile(relPath: String): File {
            val direct = File(relPath)
            if (direct.exists()) return direct
            val fromHome = File("/home/sbx/work/distribute-ovpn", relPath)
            if (fromHome.exists()) return fromHome
            return File(rootDir, relPath)
        }

        val consoleDist = resolveFile("backend/console-ui/dist")
        val assetsDist = resolveFile("backend/console-ui/dist/assets")
        val prototypesDir = resolveFile("docs/prototypes")

        if (assetsDist.exists()) {
            staticFiles("/assets", assetsDist)
        } else if (consoleDist.exists()) {
            val subAssets = File(consoleDist, "assets")
            if (subAssets.exists()) staticFiles("/assets", subAssets)
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

        // --- Root Welcome & UI Redirect ---
        get("/") {
            val distIndex = resolveFile("backend/console-ui/dist/index.html")
            if (distIndex.exists()) {
                call.respondFile(distIndex)
            } else {
                call.respondRedirect("/console", permanent = false)
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

        // --- Authentication & TOTP API ---
        route("/api/v1/auth") {
            post("/register") {
                val req = call.receive<RegisterUserRequest>()
                val result = service.registerUser(req.username, req.password, req.role)
                result.fold(
                    onSuccess = { user ->
                        call.respond(HttpStatusCode.Created, ApiEnvelope.ok(user))
                    },
                    onFailure = { err ->
                        call.respond(HttpStatusCode.BadRequest, ApiEnvelope.failure<Unit>(ErrorCode.INVALID_REQUEST, err.message ?: "Failed to register user"))
                    }
                )
            }

            get("/totp/setup") {
                val username = call.request.queryParameters["username"]
                if (username.isNullOrEmpty()) {
                    return@get call.respond(HttpStatusCode.BadRequest, ApiEnvelope.failure<Unit>(ErrorCode.INVALID_REQUEST, "Missing username parameter"))
                }
                val setup = service.getTotpSetup(username)
                if (setup != null) {
                    call.respond(HttpStatusCode.OK, ApiEnvelope.ok(setup))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiEnvelope.failure<Unit>(ErrorCode.NOT_FOUND, "User not found"))
                }
            }

            post("/login") {
                val req = call.receive<LoginRequest>()
                val result = service.loginUser(req)
                result.fold(
                    onSuccess = { res ->
                        call.respond(HttpStatusCode.OK, ApiEnvelope.ok(res))
                    },
                    onFailure = { err ->
                        call.respond(HttpStatusCode.Unauthorized, ApiEnvelope.failure<Unit>(ErrorCode.UNAUTHORIZED, err.message ?: "Authentication failed"))
                    }
                )
            }
        }

        // --- Public / Device Enrollment API ---
        route("/api/v1/devices") {
            get {
                val devices = service.listDevices()
                call.respond(HttpStatusCode.OK, ApiEnvelope.ok(devices))
            }

            post("/authorize") {
                val req = call.receive<AuthorizeDeviceRequest>()
                val result = service.authorizeDevice(req)
                result.fold(
                    onSuccess = { res ->
                        call.respond(HttpStatusCode.Created, ApiEnvelope.ok(res))
                    },
                    onFailure = { err ->
                        call.respond(HttpStatusCode.Unauthorized, ApiEnvelope.failure<Unit>(ErrorCode.UNAUTHORIZED, err.message ?: "Device authorization failed"))
                    }
                )
            }

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

        // --- Audit Logs API ---
        route("/api/v1/audit") {
            get {
                val logs = service.listAuditLogs()
                call.respond(HttpStatusCode.OK, ApiEnvelope.ok(logs))
            }
        }

        // --- Authenticated Device Profile Sync API (Strict Online Policy A) ---
        route("/api/v1/sync") {
            get("/manifest") {
                val authHeader = call.request.headers["Authorization"]
                val token = authHeader?.removePrefix("Bearer ")?.trim()
                if (token.isNullOrEmpty()) {
                    return@get call.respond(HttpStatusCode.Unauthorized, ApiEnvelope.failure<Unit>(ErrorCode.UNAUTHORIZED, "Missing Bearer token"))
                }

                val device = service.authenticateDevice(token)
                if (device == null) {
                    return@get call.respond(HttpStatusCode.Forbidden, ApiEnvelope.failure<Unit>(ErrorCode.FORBIDDEN, "Device revoked, expired, or unauthorized"))
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
            get {
                val profiles = service.listProfiles()
                call.respond(HttpStatusCode.OK, ApiEnvelope.ok(profiles))
            }

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

        // --- Client Apps Management Routing ---
        route("/api/v1/client-apps") {

            get {
                val releases = service.listClientApps()
                call.respond(HttpStatusCode.OK, ApiEnvelope.ok(releases))
            }

            get("/latest") {
                val platformParam = call.request.queryParameters["platform"]?.uppercase()
                val platform = try {
                    if (platformParam != null) DevicePlatform.valueOf(platformParam) else null
                } catch (e: Exception) {
                    null
                }

                if (platform == null) {
                    return@get call.respond(HttpStatusCode.BadRequest, ApiEnvelope.failure<Unit>(ErrorCode.INVALID_REQUEST, "Valid platform parameter (ANDROID, MACOS, etc.) is required"))
                }

                val channelParam = call.request.queryParameters["channel"] ?: "STABLE"
                val channel = try {
                    ReleaseChannel.valueOf(channelParam.uppercase())
                } catch (e: Exception) {
                    ReleaseChannel.STABLE
                }

                val release = service.getLatestClientApp(platform, channel)
                if (release != null) {
                    call.respond(HttpStatusCode.OK, ApiEnvelope.ok(release))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiEnvelope.failure<Unit>(ErrorCode.NOT_FOUND, "No release found for $platform channel $channel"))
                }
            }

            post("/publish") {
                val req = call.receive<PublishClientAppRequest>()
                val created = service.publishClientApp(req)
                call.respond(HttpStatusCode.Created, ApiEnvelope.ok(created))
            }
        }
    }
}

fun main() {
    val dbFileDir = File("/home/sbx/data").apply { mkdirs() }
    val defaultH2Url = if (dbFileDir.exists() && dbFileDir.canWrite()) {
        "jdbc:h2:file:/home/sbx/data/vpn_registry;DB_CLOSE_DELAY=-1"
    } else {
        "jdbc:h2:file:./data/vpn_registry;DB_CLOSE_DELAY=-1"
    }

    Database.connect(
        url = System.getenv("JDBC_DATABASE_URL") ?: defaultH2Url,
        driver = System.getenv("JDBC_DRIVER") ?: "org.h2.Driver",
        user = System.getenv("DB_USER") ?: "sa",
        password = System.getenv("DB_PASSWORD") ?: ""
    )

    transaction {
        SchemaUtils.create(
            UsersTable,
            ProfilesTable,
            ProfileVersionsTable,
            ChannelPublicationsTable,
            DevicesTable,
            EnrollmentTokensTable,
            AuditLogsTable,
            ClientAppReleasesTable
        )

        // Auto-seed default admin if no users exist
        val hasUsers = UsersTable.selectAll().count() > 0
        if (!hasUsers) {
            val salt = PasswordService.generateSalt()
            val passwordHash = PasswordService.hashPassword("Password123!", salt)
            val now = System.currentTimeMillis()
            UsersTable.insert {
                it[id] = "usr-admin-seed"
                it[username] = "admin"
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.salt] = salt
                it[totpSecret] = "SGZUJEORAE4DNP5UZNVQMX2N6QH2MD2S"
                it[role] = UserRole.ADMIN.name
                it[status] = UserStatus.ACTIVE.name
                it[createdAtEpochMs] = now
                it[updatedAtEpochMs] = now
            }
        }

        // Auto-seed default profile if empty
        val hasProfiles = ProfilesTable.selectAll().count() > 0
        if (!hasProfiles) {
            ProfilesTable.insert {
                it[id] = "default"
                it[name] = "Default Profile"
                it[description] = "Standard corporate gateway profile"
                it[createdAtEpochMs] = System.currentTimeMillis()
            }
        }
    }

    embeddedServer(Netty, port = System.getenv("PORT")?.toIntOrNull() ?: 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}
