package fyi.rifqifardi.vpn.backend.service

import fyi.rifqifardi.vpn.backend.db.ChannelPublicationsTable
import fyi.rifqifardi.vpn.backend.db.ProfileVersionsTable
import fyi.rifqifardi.vpn.backend.db.ProfilesTable
import fyi.rifqifardi.vpn.model.*
import fyi.rifqifardi.vpn.policy.OvpnParserAndPolicyValidator
import fyi.rifqifardi.vpn.policy.PolicyValidationResult
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.util.UUID

class ProfileRegistryService(
    private val validator: OvpnParserAndPolicyValidator = OvpnParserAndPolicyValidator()
) {

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
        ovpnContent: String,
        signature: String = "simulated_sig",
        keyId: String = "key-v1"
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
                // Determine next version number
                val latestVersion = ProfileVersionsTable
                    .selectAll()
                    .where { ProfileVersionsTable.profileId eq profileId }
                    .maxOfOrNull { it[ProfileVersionsTable.version] } ?: 0L

                val nextVersion = latestVersion + 1
                val digest = calculateSha256(ovpnContent.toByteArray(Charsets.UTF_8))
                val versionRecordId = UUID.randomUUID().toString()

                ProfileVersionsTable.insert {
                    it[id] = versionRecordId
                    it[ProfileVersionsTable.profileId] = profileId
                    it[version] = nextVersion
                    it[ProfileVersionsTable.ovpnContent] = ovpnContent
                    it[sha256Digest] = digest
                    it[ProfileVersionsTable.signature] = signature
                    it[ProfileVersionsTable.keyId] = keyId
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

    private fun calculateSha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
