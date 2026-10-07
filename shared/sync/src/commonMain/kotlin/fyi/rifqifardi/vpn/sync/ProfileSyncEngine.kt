package fyi.rifqifardi.vpn.sync

import fyi.rifqifardi.vpn.crypto.DigestCalculator
import fyi.rifqifardi.vpn.crypto.SignatureVerifier
import fyi.rifqifardi.vpn.domain.contracts.FileStore
import fyi.rifqifardi.vpn.domain.contracts.SecureStorage
import fyi.rifqifardi.vpn.domain.contracts.ValidatedProfile
import fyi.rifqifardi.vpn.model.*
import fyi.rifqifardi.vpn.policy.OvpnParserAndPolicyValidator
import fyi.rifqifardi.vpn.policy.PolicyValidationResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

sealed class SyncState {
    data object Idle : SyncState()
    data object Checking : SyncState()
    data class Downloading(val version: Long) : SyncState()
    data class UpToDate(val version: Long) : SyncState()
    data class Staged(val profile: ValidatedProfile) : SyncState()
    data class Error(val message: String) : SyncState()
}

class ProfileSyncEngine(
    private val baseUrl: String,
    private val secureStorage: SecureStorage,
    private val fileStore: FileStore,
    private val policyValidator: OvpnParserAndPolicyValidator,
    private val signatureVerifier: SignatureVerifier,
    private val digestCalculator: DigestCalculator,
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    suspend fun sync(): SyncResult {
        _syncState.value = SyncState.Checking

        val apiKey = secureStorage.getString("device_api_key")
            ?: return syncError("Device is not enrolled (missing API key)")
        val serverSigningKey = secureStorage.getString("server_signing_public_key")
            ?: return syncError("Device is missing server signing key")

        return try {
            val resp = httpClient.get("$baseUrl/api/v1/sync/manifest") {
                header("Authorization", "Bearer $apiKey")
            }

            if (resp.status == HttpStatusCode.Forbidden) {
                return syncError("Device has been revoked")
            }

            if (!resp.status.isSuccess()) {
                return syncError("Failed to fetch manifest: ${resp.status}")
            }

            val envelope = resp.body<ApiEnvelope<ProfileManifest>>()
            val manifest = envelope.data ?: return syncError("No manifest data returned")

            val currentActiveVersion = secureStorage.getString("active_version")?.toLongOrNull() ?: 0L
            if (manifest.version <= currentActiveVersion) {
                _syncState.value = SyncState.UpToDate(currentActiveVersion)
                return SyncResult.Success(null, "Up to date at version $currentActiveVersion")
            }

            _syncState.value = SyncState.Downloading(manifest.version)
            val artifactResp = httpClient.get(manifest.downloadUrl) {
                header("Authorization", "Bearer $apiKey")
            }

            if (!artifactResp.status.isSuccess()) {
                return syncError("Failed to download artifact: ${artifactResp.status}")
            }

            val ovpnContent = artifactResp.body<String>()
            val ovpnBytes = ovpnContent.encodeToByteArray()

            // 1. Digest Verification
            val calculatedDigest = digestCalculator.calculateSha256(ovpnBytes)
            if (calculatedDigest.hexValue != manifest.digest.hexValue) {
                return syncError("SHA-256 digest mismatch! Expected ${manifest.digest.hexValue}, got ${calculatedDigest.hexValue}")
            }

            // 2. Cryptographic Signature Verification
            val isSigValid = signatureVerifier.verify(ovpnBytes, manifest.signature, serverSigningKey)
            if (!isSigValid) {
                return syncError("Cryptographic signature verification failed for version ${manifest.version}")
            }

            // 3. Security Policy & Parser Validation
            when (val policyRes = policyValidator.validate(ovpnContent)) {
                is PolicyValidationResult.Invalid -> {
                    return syncError("Policy violation: ${policyRes.errors.joinToString()}")
                }
                is PolicyValidationResult.Valid -> {
                    val validated = ValidatedProfile(
                        manifest = manifest,
                        ovpnConfigContent = ovpnContent,
                        validatedAtEpochMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                    )

                    // Stage artifact file locally
                    val stagedPath = "staged_v${manifest.version}.ovpn"
                    fileStore.writeBytes(stagedPath, ovpnBytes)
                    secureStorage.setString("staged_version", manifest.version.toString())

                    _syncState.value = SyncState.Staged(validated)
                    SyncResult.Success(validated, "Version ${manifest.version} staged successfully")
                }
            }
        } catch (e: Exception) {
            syncError("Sync exception: ${e.message}")
        }
    }

    private fun syncError(message: String): SyncResult {
        _syncState.value = SyncState.Error(message)
        return SyncResult.Failure(message)
    }
}

sealed class SyncResult {
    data class Success(val stagedProfile: ValidatedProfile?, val message: String) : SyncResult()
    data class Failure(val error: String) : SyncResult()
}
