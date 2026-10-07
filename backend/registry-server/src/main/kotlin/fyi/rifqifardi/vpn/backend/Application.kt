package fyi.rifqifardi.vpn.backend

import fyi.rifqifardi.vpn.backend.db.*
import fyi.rifqifardi.vpn.backend.service.ProfileRegistryService
import fyi.rifqifardi.vpn.model.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
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
            EnrollmentTokensTable
        )
    }

    embeddedServer(Netty, port = System.getenv("PORT")?.toIntOrNull() ?: 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}
