package fyi.rifqifardi.vpn.cli

import fyi.rifqifardi.vpn.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File

class VpnCtlClient(
    private val serverUrl: String = System.getenv("VPN_REGISTRY_URL") ?: "http://localhost:8080"
) {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { prettyPrint = true; ignoreUnknownKeys = true })
        }
    }

    fun createProfile(id: String, name: String, description: String?): Boolean = runBlocking {
        val resp = httpClient.post("$serverUrl/api/v1/profiles") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("id" to id, "name" to name, "description" to (description ?: "")))
        }
        resp.status.isSuccess()
    }

    fun uploadProfileVersion(profileId: String, ovpnFilePath: String): Long? = runBlocking {
        val file = File(ovpnFilePath)
        if (!file.exists()) {
            println("Error: File not found at $ovpnFilePath")
            return@runBlocking null
        }
        val content = file.readText()
        val resp = httpClient.post("$serverUrl/api/v1/profiles/$profileId/versions") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("ovpnContent" to content))
        }
        if (resp.status.isSuccess()) {
            val env = resp.body<ApiEnvelope<Map<String, Long>>>()
            env.data?.get("version")
        } else {
            println("Upload failed: ${resp.status}")
            null
        }
    }

    fun createEnrollmentToken(
        deviceName: String,
        platform: DevicePlatform,
        profileId: String,
        channel: ReleaseChannel
    ): EnrollmentTokenResponse? = runBlocking {
        val resp = httpClient.post("$serverUrl/api/v1/devices/tokens") {
            contentType(ContentType.Application.Json)
            setBody(
                EnrollmentTokenRequest(
                    deviceName = deviceName,
                    platform = platform,
                    assignedProfileId = profileId,
                    assignedChannel = channel
                )
            )
        }
        if (resp.status.isSuccess()) {
            val env = resp.body<ApiEnvelope<EnrollmentTokenResponse>>()
            env.data
        } else {
            println("Token generation failed: ${resp.status}")
            null
        }
    }

    fun revokeDevice(deviceId: String): Boolean = runBlocking {
        val resp = httpClient.post("$serverUrl/api/v1/devices/$deviceId/revoke")
        resp.status.isSuccess()
    }
}

fun printUsage() {
    println("""
        vpnctl — KMP VPN Platform Administration CLI
        
        Usage:
          vpnctl profile create <id> <name> [description]
          vpnctl profile upload <profile-id> <path-to.ovpn>
          vpnctl device token <device-name> <platform: ANDROID|MACOS> <profile-id> [STABLE|BETA]
          vpnctl device revoke <device-id>
    """.trimIndent())
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        return
    }

    val client = VpnCtlClient()

    when (args[0]) {
        "profile" -> {
            if (args.size < 2) { printUsage(); return }
            when (args[1]) {
                "create" -> {
                    if (args.size < 4) { println("Usage: vpnctl profile create <id> <name> [desc]"); return }
                    val ok = client.createProfile(args[2], args[3], args.getOrNull(4))
                    println(if (ok) "Profile '${args[2]}' created successfully." else "Failed to create profile.")
                }
                "upload" -> {
                    if (args.size < 4) { println("Usage: vpnctl profile upload <profile-id> <file.ovpn>"); return }
                    val ver = client.uploadProfileVersion(args[2], args[3])
                    if (ver != null) {
                        println("Profile version $ver uploaded and verified.")
                    }
                }
            }
        }
        "device" -> {
            if (args.size < 2) { printUsage(); return }
            when (args[1]) {
                "token" -> {
                    if (args.size < 5) { println("Usage: vpnctl device token <device-name> <platform> <profile-id> [channel]"); return }
                    val platform = DevicePlatform.valueOf(args[3].uppercase())
                    val channel = ReleaseChannel.valueOf(args.getOrNull(5)?.uppercase() ?: "STABLE")
                    val res = client.createEnrollmentToken(args[2], platform, args[4], channel)
                    if (res != null) {
                        println("Enrollment Token: ${res.token}")
                        println("Expires At (Epoch): ${res.expiresAtEpochMs}")
                    }
                }
                "revoke" -> {
                    if (args.size < 3) { println("Usage: vpnctl device revoke <device-id>"); return }
                    val ok = client.revokeDevice(args[2])
                    println(if (ok) "Device '${args[2]}' revoked." else "Failed to revoke device.")
                }
            }
        }
        else -> printUsage()
    }
}
