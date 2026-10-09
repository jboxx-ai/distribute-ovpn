package fyi.rifqifardi.vpn.backend.service

import fyi.rifqifardi.vpn.backend.auth.PasswordService
import fyi.rifqifardi.vpn.backend.auth.TotpService
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

    // --- User & TOTP Authentication ---

    fun registerUser(
        username: String,
        password: String,
        role: UserRole = UserRole.OPERATOR
    ): Result<UserProfile> = transaction {
        val existing = UsersTable.selectAll().where { UsersTable.username eq username }.count() > 0
        if (existing) {
            return@transaction Result.failure(IllegalArgumentException("User '$username' already exists"))
        }

        val userId = "usr-" + UUID.randomUUID().toString().take(12)
        val salt = PasswordService.generateSalt()
        val passwordHash = PasswordService.hashPassword(password, salt)
        val totpSecret = TotpService.generateSecret()
        val now = System.currentTimeMillis()

        UsersTable.insert {
            it[id] = userId
            it[UsersTable.username] = username
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.salt] = salt
            it[UsersTable.totpSecret] = totpSecret
            it[UsersTable.role] = role.name
            it[status] = UserStatus.ACTIVE.name
            it[createdAtEpochMs] = now
            it[updatedAtEpochMs] = now
        }

        Result.success(
            UserProfile(
                id = userId,
                username = username,
                role = role,
                status = UserStatus.ACTIVE
            )
        )
    }

    fun getTotpSetup(username: String): TotpSetupResponse? = transaction {
        val user = UsersTable.selectAll().where { UsersTable.username eq username }.singleOrNull()
            ?: return@transaction null

        val secret = user[UsersTable.totpSecret]
        val uri = TotpService.generateOtpUri(username, secret)
        TotpSetupResponse(
            username = username,
            secret = secret,
            qrCodeUri = uri
        )
    }

    fun loginUser(req: LoginRequest): Result<LoginResponse> = transaction {
        val user = UsersTable.selectAll().where { UsersTable.username eq req.username }.singleOrNull()
            ?: return@transaction Result.failure(IllegalArgumentException("Invalid username or password"))

        val status = UserStatus.valueOf(user[UsersTable.status])
        if (status != UserStatus.ACTIVE) {
            return@transaction Result.failure(IllegalStateException("User account is disabled"))
        }

        val salt = user[UsersTable.salt]
        val expectedHash = user[UsersTable.passwordHash]
        if (!PasswordService.verifyPassword(req.password, salt, expectedHash)) {
            return@transaction Result.failure(IllegalArgumentException("Invalid username or password"))
        }

        val totpSecret = user[UsersTable.totpSecret]
        if (!TotpService.verifyCode(totpSecret, req.totpCode)) {
            return@transaction Result.failure(IllegalArgumentException("Invalid or expired TOTP code"))
        }

        val token = "usr_sess_" + UUID.randomUUID().toString().replace("-", "")
        val expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000L) // 24 hours

        Result.success(
            LoginResponse(
                token = token,
                user = UserProfile(
                    id = user[UsersTable.id],
                    username = user[UsersTable.username],
                    role = UserRole.valueOf(user[UsersTable.role]),
                    status = status
                ),
                expiresAtEpochMs = expiresAt
            )
        )
    }

    // --- Device Authorization with User TOTP (Phase 11 Primary Enrollment) ---

    fun authorizeDevice(req: AuthorizeDeviceRequest): Result<DeviceAuthorizationResponse> = transaction {
        val user = UsersTable.selectAll().where { UsersTable.username eq req.username }.singleOrNull()
            ?: return@transaction Result.failure(IllegalArgumentException("Invalid username or password"))

        val userStatus = UserStatus.valueOf(user[UsersTable.status])
        if (userStatus != UserStatus.ACTIVE) {
            return@transaction Result.failure(IllegalStateException("User account is disabled"))
        }

        val salt = user[UsersTable.salt]
        val expectedHash = user[UsersTable.passwordHash]
        if (!PasswordService.verifyPassword(req.password, salt, expectedHash)) {
            return@transaction Result.failure(IllegalArgumentException("Invalid username or password"))
        }

        val totpSecret = user[UsersTable.totpSecret]
        if (!TotpService.verifyCode(totpSecret, req.totpCode)) {
            return@transaction Result.failure(IllegalArgumentException("Invalid or expired TOTP code"))
        }

        val profileExists = ProfilesTable.selectAll().where { ProfilesTable.id eq req.assignedProfileId }.count() > 0
        if (!profileExists) {
            return@transaction Result.failure(IllegalArgumentException("Profile '${req.assignedProfileId}' not found"))
        }

        val userId = user[UsersTable.id]
        val deviceId = "dev-" + UUID.randomUUID().toString().take(12)
        val deviceToken = "vpn_dev_" + UUID.randomUUID().toString().replace("-", "")
        val expiresAt = System.currentTimeMillis() + (req.tokenValidityDays * 24 * 60 * 60 * 1000L)
        val now = System.currentTimeMillis()

        DevicesTable.insert {
            it[id] = deviceId
            it[name] = req.deviceName
            it[platform] = req.platform.name
            it[apiKey] = deviceToken
            it[status] = DeviceStatus.ACTIVE.name
            it[assignedProfileId] = req.assignedProfileId
            it[assignedChannel] = req.assignedChannel.name
            it[enrolledAtEpochMs] = now
            it[lastSeenAtEpochMs] = now
            it[DevicesTable.userId] = userId
            it[devicePublicKey] = req.devicePublicKeyBase64
            it[tokenExpiresAtEpochMs] = expiresAt
        }

        Result.success(
            DeviceAuthorizationResponse(
                deviceId = deviceId,
                deviceToken = deviceToken,
                userId = userId,
                assignedProfileId = req.assignedProfileId,
                assignedChannel = req.assignedChannel,
                signingPublicKeyBase64 = publicKeyBase64,
                expiresAtEpochMs = expiresAt
            )
        )
    }

    // --- Profile Management ---

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

    // --- Legacy Token-Based Enrollment (Kept for backwards compatibility) ---

    fun createEnrollmentToken(req: EnrollmentTokenRequest): EnrollmentTokenResponse = transaction {
        val targetProfileId = if (req.assignedProfileId.isNotBlank()) req.assignedProfileId else "default"
        
        // Ensure the profile exists to prevent foreign key violation
        val profileExists = ProfilesTable.selectAll().where { ProfilesTable.id eq targetProfileId }.count() > 0
        if (!profileExists) {
            val fallbackProfile = ProfilesTable.selectAll().firstOrNull()
            if (fallbackProfile != null) {
                // If the specified profile doesn't exist, fallback to first available profile
                return@transaction createEnrollmentToken(req.copy(assignedProfileId = fallbackProfile[ProfilesTable.id]))
            } else {
                // Auto-seed default profile if none exists
                val now = System.currentTimeMillis()
                ProfilesTable.insert {
                    it[id] = targetProfileId
                    it[name] = if (targetProfileId == "default") "Default Profile" else targetProfileId
                    it[description] = "Auto-generated default profile"
                    it[createdAtEpochMs] = now
                }
            }
        }

        val token = UUID.randomUUID().toString().replace("-", "")
        val expiresAt = System.currentTimeMillis() + (req.expiresInMinutes * 60 * 1000)

        EnrollmentTokensTable.insert {
            it[EnrollmentTokensTable.token] = token
            it[deviceName] = req.deviceName
            it[platform] = req.platform.name
            it[assignedProfileId] = targetProfileId
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
            it[devicePublicKey] = req.devicePublicKeyBase64
        }

        EnrolledDeviceCredentials(
            deviceId = deviceId,
            apiKey = apiKey,
            assignedProfileId = profileId,
            assignedChannel = channel,
            signingPublicKeyBase64 = publicKeyBase64
        )
    }

    // --- Strict Online Verification (Policy A) ---

    fun authenticateDevice(tokenOrApiKey: String): DeviceInfo? = transaction {
        val row = DevicesTable
            .selectAll()
            .where { DevicesTable.apiKey eq tokenOrApiKey }
            .singleOrNull() ?: return@transaction null

        val status = DeviceStatus.valueOf(row[DevicesTable.status])
        if (status != DeviceStatus.ACTIVE) return@transaction null

        val tokenExpires = row[DevicesTable.tokenExpiresAtEpochMs]
        if (tokenExpires != null && System.currentTimeMillis() > tokenExpires) {
            return@transaction null
        }

        val uId = row[DevicesTable.userId]
        if (uId != null) {
            val userRow = UsersTable.selectAll().where { UsersTable.id eq uId }.singleOrNull()
            if (userRow != null) {
                val userStatus = UserStatus.valueOf(userRow[UsersTable.status])
                if (userStatus != UserStatus.ACTIVE) return@transaction null
            }
        }

        val now = System.currentTimeMillis()
        DevicesTable.update({ DevicesTable.apiKey eq tokenOrApiKey }) {
            it[lastSeenAtEpochMs] = now
        }

        DeviceInfo(
            deviceId = row[DevicesTable.id],
            deviceName = row[DevicesTable.name],
            platform = DevicePlatform.valueOf(row[DevicesTable.platform]),
            status = status,
            assignedProfileId = row[DevicesTable.assignedProfileId],
            assignedChannel = ReleaseChannel.valueOf(row[DevicesTable.assignedChannel]),
            enrolledAtEpochMs = row[DevicesTable.enrolledAtEpochMs],
            lastSeenAtEpochMs = now,
            userId = uId,
            tokenExpiresAtEpochMs = tokenExpires
        )
    }

    fun revokeDevice(deviceId: String): Boolean = transaction {
        val updated = DevicesTable.update({ DevicesTable.id eq deviceId }) {
            it[status] = DeviceStatus.REVOKED.name
        }
        updated > 0
    }

    fun listProfiles(): List<ProfileSummary> = transaction {
        val profiles = ProfilesTable.selectAll().toList()
        profiles.map { row ->
            val pId = row[ProfilesTable.id]
            val versionsCount = ProfileVersionsTable.selectAll().where { ProfileVersionsTable.profileId eq pId }.count()
            val latestVer = ProfileVersionsTable.selectAll().where { ProfileVersionsTable.profileId eq pId }
                .maxOfOrNull { it[ProfileVersionsTable.version] } ?: 0L
            
            val stablePub = ChannelPublicationsTable.selectAll()
                .where { (ChannelPublicationsTable.profileId eq pId) and (ChannelPublicationsTable.channel eq "STABLE") }
                .singleOrNull()
            val stableVer = stablePub?.let { pub ->
                ProfileVersionsTable.selectAll().where { ProfileVersionsTable.id eq pub[ChannelPublicationsTable.activeVersionId] }
                    .singleOrNull()?.get(ProfileVersionsTable.version)
            }

            val betaPub = ChannelPublicationsTable.selectAll()
                .where { (ChannelPublicationsTable.profileId eq pId) and (ChannelPublicationsTable.channel eq "BETA") }
                .singleOrNull()
            val betaVer = betaPub?.let { pub ->
                ProfileVersionsTable.selectAll().where { ProfileVersionsTable.id eq pub[ChannelPublicationsTable.activeVersionId] }
                    .singleOrNull()?.get(ProfileVersionsTable.version)
            }

            val canaryPub = ChannelPublicationsTable.selectAll()
                .where { (ChannelPublicationsTable.profileId eq pId) and (ChannelPublicationsTable.channel eq "CANARY") }
                .singleOrNull()
            val canaryVer = canaryPub?.let { pub ->
                ProfileVersionsTable.selectAll().where { ProfileVersionsTable.id eq pub[ChannelPublicationsTable.activeVersionId] }
                    .singleOrNull()?.get(ProfileVersionsTable.version)
            }

            ProfileSummary(
                id = pId,
                name = row[ProfilesTable.name],
                description = row[ProfilesTable.description],
                createdAtEpochMs = row[ProfilesTable.createdAtEpochMs],
                totalVersions = versionsCount,
                latestVersion = latestVer,
                stableVersion = stableVer,
                betaVersion = betaVer,
                canaryVersion = canaryVer
            )
        }
    }

    fun listDevices(): List<DeviceInfo> = transaction {
        DevicesTable.selectAll().map { row ->
            DeviceInfo(
                deviceId = row[DevicesTable.id],
                deviceName = row[DevicesTable.name],
                platform = DevicePlatform.valueOf(row[DevicesTable.platform]),
                status = DeviceStatus.valueOf(row[DevicesTable.status]),
                assignedProfileId = row[DevicesTable.assignedProfileId],
                assignedChannel = ReleaseChannel.valueOf(row[DevicesTable.assignedChannel]),
                enrolledAtEpochMs = row[DevicesTable.enrolledAtEpochMs],
                lastSeenAtEpochMs = row[DevicesTable.lastSeenAtEpochMs],
                userId = row[DevicesTable.userId],
                tokenExpiresAtEpochMs = row[DevicesTable.tokenExpiresAtEpochMs]
            )
        }
    }

    fun listAuditLogs(): List<AuditLogEntry> = transaction {
        fyi.rifqifardi.vpn.backend.audit.AuditLogsTable.selectAll().orderBy(fyi.rifqifardi.vpn.backend.audit.AuditLogsTable.timestampEpochMs to SortOrder.DESC).limit(50).map { row ->
            AuditLogEntry(
                id = row[fyi.rifqifardi.vpn.backend.audit.AuditLogsTable.id],
                actor = row[fyi.rifqifardi.vpn.backend.audit.AuditLogsTable.actor],
                action = row[fyi.rifqifardi.vpn.backend.audit.AuditLogsTable.action],
                targetType = row[fyi.rifqifardi.vpn.backend.audit.AuditLogsTable.targetType],
                targetId = row[fyi.rifqifardi.vpn.backend.audit.AuditLogsTable.targetId],
                detailsSanitized = row[fyi.rifqifardi.vpn.backend.audit.AuditLogsTable.detailsSanitized],
                timestampEpochMs = row[fyi.rifqifardi.vpn.backend.audit.AuditLogsTable.timestampEpochMs]
            )
        }
    }

    // --- Client Apps Management ---

    fun publishClientApp(req: PublishClientAppRequest): ClientAppRelease = transaction {
        val relId = "rel-" + UUID.randomUUID().toString().take(12)
        val now = System.currentTimeMillis()

        // Upsert or replace existing platform/version/channel
        ClientAppReleasesTable.deleteWhere {
            (ClientAppReleasesTable.platform eq req.platform.name) and
            (ClientAppReleasesTable.version eq req.version) and
            (ClientAppReleasesTable.channel eq req.channel.name)
        }

        ClientAppReleasesTable.insert {
            it[id] = relId
            it[platform] = req.platform.name
            it[version] = req.version
            it[channel] = req.channel.name
            it[fileName] = req.fileName
            it[downloadUrl] = req.downloadUrl
            it[fileSizeBytes] = req.fileSizeBytes
            it[sha256Checksum] = req.sha256Checksum
            it[minOsVersion] = req.minOsVersion
            it[releaseNotes] = req.releaseNotes
            it[publishedAtEpochMs] = now
        }

        ClientAppRelease(
            id = relId,
            platform = req.platform,
            version = req.version,
            channel = req.channel,
            fileName = req.fileName,
            downloadUrl = req.downloadUrl,
            fileSizeBytes = req.fileSizeBytes,
            sha256Checksum = req.sha256Checksum,
            minOsVersion = req.minOsVersion,
            releaseNotes = req.releaseNotes,
            publishedAtEpochMs = now
        )
    }

    fun listClientApps(): List<ClientAppRelease> = transaction {
        ClientAppReleasesTable.selectAll().orderBy(ClientAppReleasesTable.publishedAtEpochMs to SortOrder.DESC).map { row ->
            ClientAppRelease(
                id = row[ClientAppReleasesTable.id],
                platform = DevicePlatform.valueOf(row[ClientAppReleasesTable.platform]),
                version = row[ClientAppReleasesTable.version],
                channel = ReleaseChannel.valueOf(row[ClientAppReleasesTable.channel]),
                fileName = row[ClientAppReleasesTable.fileName],
                downloadUrl = row[ClientAppReleasesTable.downloadUrl],
                fileSizeBytes = row[ClientAppReleasesTable.fileSizeBytes],
                sha256Checksum = row[ClientAppReleasesTable.sha256Checksum],
                minOsVersion = row[ClientAppReleasesTable.minOsVersion],
                releaseNotes = row[ClientAppReleasesTable.releaseNotes],
                publishedAtEpochMs = row[ClientAppReleasesTable.publishedAtEpochMs]
            )
        }
    }

    fun getLatestClientApp(platform: DevicePlatform, channel: ReleaseChannel = ReleaseChannel.STABLE): ClientAppRelease? = transaction {
        ClientAppReleasesTable.selectAll()
            .where { (ClientAppReleasesTable.platform eq platform.name) and (ClientAppReleasesTable.channel eq channel.name) }
            .orderBy(ClientAppReleasesTable.publishedAtEpochMs to SortOrder.DESC)
            .limit(1)
            .map { row ->
                ClientAppRelease(
                    id = row[ClientAppReleasesTable.id],
                    platform = DevicePlatform.valueOf(row[ClientAppReleasesTable.platform]),
                    version = row[ClientAppReleasesTable.version],
                    channel = ReleaseChannel.valueOf(row[ClientAppReleasesTable.channel]),
                    fileName = row[ClientAppReleasesTable.fileName],
                    downloadUrl = row[ClientAppReleasesTable.downloadUrl],
                    fileSizeBytes = row[ClientAppReleasesTable.fileSizeBytes],
                    sha256Checksum = row[ClientAppReleasesTable.sha256Checksum],
                    minOsVersion = row[ClientAppReleasesTable.minOsVersion],
                    releaseNotes = row[ClientAppReleasesTable.releaseNotes],
                    publishedAtEpochMs = row[ClientAppReleasesTable.publishedAtEpochMs]
                )
            }.singleOrNull()
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
