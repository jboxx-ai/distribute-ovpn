package fyi.rifqifardi.vpn.backend.service

import fyi.rifqifardi.vpn.backend.db.*
import fyi.rifqifardi.vpn.model.*
import fyi.rifqifardi.vpn.policy.OvpnParserAndPolicyValidator
import fyi.rifqifardi.vpn.policy.PolicyValidationResult
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64
import java.util.UUID

class ProfileRegistryService(
    private val validator: OvpnParserAndPolicyValidator = OvpnParserAndPolicyValidator(),
    private val keyPair: KeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair(),
    val activeKeyId: String = "srv-ed25519-v1"
) {

    val publicKeyBase64: String = Base64.getEncoder().encodeToString(keyPair.public.encoded)

    fun createProfile(id: String, name: String, description: String?): Boolean = transaction {
        val exists = ProfilesTable.selectAll().where { ProfilesTable.id eq id }.count() > 0
        if (exists) return@transaction false

        ProfilesTable.insert {
            it[ProfilesTable.id] = id
            it[ProfilesTable.name] = name
            it[ProfilesTable.description] = description
            it[ProfilesTable.createdAtEpochMs] = System.currentTimeMillis()
        }
        true
    }

    fun uploadVersion(
        profileId: String,
        ovpnContent: String
    ): Result<Long> = transaction {
        val profileExists = ProfilesTable.selectAll().where { ProfilesTable.id eq profileId }.count() > 0
        if (!profileExists) {
            return@transaction Result.failure(IllegalArgumentException("Profile '$profileId' not found"))
        }

        when (val validation = validator.validate(ovpnContent)) {
            is PolicyValidationResult.Invalid -> {
                return@transaction Result.failure(IllegalArgumentException("Policy validation failed: ${validation.errors.joinToString()}"))
            }
            is PolicyValidationResult.Valid -> {
                val latestVersion = ProfileVersionsTable
                    .selectAll()
                    .where { ProfileVersionsTable.profileId eq profileId }
                    .maxOfOrNull { it[ProfileVersionsTable.version] } ?: 0L

                val nextVersion = latestVersion + 1
                val contentBytes = ovpnContent.toByteArray(Charsets.UTF_8)
                val digest = calculateSha256(contentBytes)
                val signature = signPayload(contentBytes)
                val versionRecordId = UUID.randomUUID().toString()

                ProfileVersionsTable.insert {
                    it[id] = versionRecordId
                    it[ProfileVersionsTable.profileId] = profileId
                    it[version] = nextVersion
                    it[ProfileVersionsTable.ovpnContent] = ovpnContent
                    it[sha256Digest] = digest
                    it[ProfileVersionsTable.signature] = signature
                    it[ProfileVersionsTable.keyId] = activeKeyId
                    it[createdAtEpochMs] = System.currentTimeMillis()
                }

                Result.success(nextVersion)
            }
        }
    }

    fun publishToChannel(profileId: String, version: Long, channel: ReleaseChannel): Boolean = transaction {
        val versionRow = ProfileVersionsTable
            .selectAll()
            .where { (ProfileVersionsTable.profileId eq profileId) and (ProfileVersionsTable.version eq version) }
            .singleOrNull() ?: return@transaction false

        val versionId = versionRow[ProfileVersionsTable.id]

        ChannelPublicationsTable.upsert {
            it[ChannelPublicationsTable.channel] = channel.name
            it[ChannelPublicationsTable.profileId] = profileId
            it[activeVersionId] = versionId
            it[updatedAtEpochMs] = System.currentTimeMillis()
        }
        true
    }

    fun getManifest(profileId: String, channel: ReleaseChannel, baseUrl: String): ProfileManifest? = transaction {
        val pubRow = ChannelPublicationsTable
            .selectAll()
            .where { (ChannelPublicationsTable.profileId eq profileId) and (ChannelPublicationsTable.channel eq channel.name) }
            .singleOrNull() ?: return@transaction null

        val versionId = pubRow[ChannelPublicationsTable.activeVersionId]
        val vRow = ProfileVersionsTable
            .selectAll()
            .where { ProfileVersionsTable.id eq versionId }
            .singleOrNull() ?: return@transaction null

        val ver = vRow[ProfileVersionsTable.version]
        ProfileManifest(
            schemaVersion = 1,
            profileId = profileId,
            version = ver,
            channel = channel,
            digest = ProfileDigest(algorithm = "SHA-256", hexValue = vRow[ProfileVersionsTable.sha256Digest]),
            signature = ProfileSignature(algorithm = "Ed25519", keyId = vRow[ProfileVersionsTable.keyId], signatureBase64 = vRow[ProfileVersionsTable.signature]),
            downloadUrl = "$baseUrl/api/v1/profiles/$profileId/versions/$ver/artifact",
            createdAtEpochMs = vRow[ProfileVersionsTable.createdAtEpochMs]
        )
    }

    fun getArtifact(profileId: String, version: Long): String? = transaction {
        ProfileVersionsTable
            .selectAll()
            .where { (ProfileVersionsTable.profileId eq profileId) and (ProfileVersionsTable.version eq version) }
            .singleOrNull()
            ?.get(ProfileVersionsTable.ovpnContent)
    }

    // --- Device Management & Enrollment ---

    fun createEnrollmentToken(req: EnrollmentTokenRequest): EnrollmentTokenResponse = transaction {
        val token = UUID.randomUUID().toString().replace("-", "")
        val expiresAt = System.currentTimeMillis() + (req.expiresInMinutes * 60 * 1000)

        EnrollmentTokensTable.insert {
            it[EnrollmentTokensTable.token] = token
            it[deviceName] = req.deviceName
            it[platform] = req.platform.name
            it[assignedProfileId] = req.assignedProfileId
            it[assignedChannel] = req.assignedChannel.name
            it[expiresAtEpochMs] = expiresAt
            it[used] = false
        }

        EnrollmentTokenResponse(token = token, expiresAtEpochMs = expiresAt)
    }

    fun enrollDevice(req: EnrollDeviceRequest): EnrolledDeviceCredentials? = transaction {
        val tokenRow = EnrollmentTokensTable
            .selectAll()
            .where { (EnrollmentTokensTable.token eq req.token) and (EnrollmentTokensTable.used eq false) }
            .singleOrNull() ?: return@transaction null

        val expiresAt = tokenRow[EnrollmentTokensTable.expiresAtEpochMs]
        if (System.currentTimeMillis() > expiresAt) {
            return@transaction null
        }

        // Mark token used
        EnrollmentTokensTable.update({ EnrollmentTokensTable.token eq req.token }) {
            it[used] = true
        }

        val deviceId = "dev-" + UUID.randomUUID().toString().take(12)
        val apiKey = "vpn_sec_" + UUID.randomUUID().toString().replace("-", "")
        val profileId = tokenRow[EnrollmentTokensTable.assignedProfileId]
        val channelStr = tokenRow[EnrollmentTokensTable.assignedChannel]
        val channel = ReleaseChannel.valueOf(channelStr)

        DevicesTable.insert {
            it[id] = deviceId
            it[name] = tokenRow[EnrollmentTokensTable.deviceName]
            it[platform] = tokenRow[EnrollmentTokensTable.platform]
            it[DevicesTable.apiKey] = apiKey
            it[status] = DeviceStatus.ACTIVE.name
            it[assignedProfileId] = profileId
            it[assignedChannel] = channel.name
            it[enrolledAtEpochMs] = System.currentTimeMillis()
            it[lastSeenAtEpochMs] = System.currentTimeMillis()
        }

        EnrolledDeviceCredentials(
            deviceId = deviceId,
            apiKey = apiKey,
            assignedProfileId = profileId,
            assignedChannel = channel,
            signingPublicKeyBase64 = publicKeyBase64
        )
    }

    fun authenticateDevice(apiKey: String): DeviceInfo? = transaction {
        val row = DevicesTable
            .selectAll()
            .where { DevicesTable.apiKey eq apiKey }
            .singleOrNull() ?: return@transaction null

        val status = DeviceStatus.valueOf(row[DevicesTable.status])
        if (status != DeviceStatus.ACTIVE) return@transaction null

        DevicesTable.update({ DevicesTable.apiKey eq apiKey }) {
            it[lastSeenAtEpochMs] = System.currentTimeMillis()
        }

        DeviceInfo(
            deviceId = row[DevicesTable.id],
            deviceName = row[DevicesTable.name],
            platform = DevicePlatform.valueOf(row[DevicesTable.platform]),
            status = status,
            assignedProfileId = row[DevicesTable.assignedProfileId],
            assignedChannel = ReleaseChannel.valueOf(row[DevicesTable.assignedChannel]),
            enrolledAtEpochMs = row[DevicesTable.enrolledAtEpochMs],
            lastSeenAtEpochMs = row[DevicesTable.lastSeenAtEpochMs]
        )
    }

    fun revokeDevice(deviceId: String): Boolean = transaction {
        val updated = DevicesTable.update({ DevicesTable.id eq deviceId }) {
            it[status] = DeviceStatus.REVOKED.name
        }
        updated > 0
    }

    private fun calculateSha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    private fun signPayload(bytes: ByteArray): String {
        val signer = Signature.getInstance("Ed25519")
        signer.initSign(keyPair.private)
        signer.update(bytes)
        return Base64.getEncoder().encodeToString(signer.sign())
    }
}
