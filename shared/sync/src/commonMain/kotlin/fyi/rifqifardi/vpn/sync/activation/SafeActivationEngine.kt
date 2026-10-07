package fyi.rifqifardi.vpn.sync.activation

import fyi.rifqifardi.vpn.domain.contracts.FileStore
import fyi.rifqifardi.vpn.domain.contracts.SecureStorage
import fyi.rifqifardi.vpn.domain.contracts.TunnelController
import fyi.rifqifardi.vpn.domain.contracts.TunnelResult
import fyi.rifqifardi.vpn.domain.contracts.ValidatedProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class ActivationJournalState {
    IDLE,
    STAGING,
    ACTIVATING,
    TESTING_HEALTH,
    COMMITTED,
    ROLLING_BACK,
    ROLLED_BACK
}

@Serializable
data class ActivationJournalEntry(
    val profileId: String,
    val candidateVersion: Long,
    val lastKnownGoodVersion: Long?,
    val state: ActivationJournalState,
    val startedAtEpochMs: Long,
    val errorMessage: String? = null
)

interface TunnelHealthProbe {
    suspend fun checkHealth(): Boolean
}

class AlwaysHealthyProbe : TunnelHealthProbe {
    override suspend fun checkHealth(): Boolean = true
}

sealed class ActivationOutcome {
    data class Success(val version: Long) : ActivationOutcome()
    data class RolledBack(val activeVersion: Long?, val error: String) : ActivationOutcome()
    data class Fatal(val error: String) : ActivationOutcome()
}

class SafeActivationEngine(
    private val secureStorage: SecureStorage,
    private val fileStore: FileStore,
    private val tunnelController: TunnelController,
    private val healthProbe: TunnelHealthProbe = AlwaysHealthyProbe()
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getJournal(): ActivationJournalEntry? {
        val raw = secureStorage.getString("activation_journal") ?: return null
        return try {
            json.decodeFromString<ActivationJournalEntry>(raw)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun saveJournal(entry: ActivationJournalEntry) {
        secureStorage.setString("activation_journal", json.encodeToString(entry))
    }

    suspend fun activate(candidateProfile: ValidatedProfile): ActivationOutcome {
        val currentActiveVersion = secureStorage.getString("active_version")?.toLongOrNull()
        val candidateVersion = candidateProfile.manifest.version

        val journal = ActivationJournalEntry(
            profileId = candidateProfile.manifest.profileId,
            candidateVersion = candidateVersion,
            lastKnownGoodVersion = currentActiveVersion,
            state = ActivationJournalState.ACTIVATING,
            startedAtEpochMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        )
        saveJournal(journal)

        // 1. Prepare tunnel with candidate
        val prepRes = tunnelController.prepare(candidateProfile)
        if (prepRes is TunnelResult.Failure) {
            return rollback(journal, "Tunnel preparation failed: ${prepRes.message}")
        }

        // 2. Connect candidate tunnel
        saveJournal(journal.copy(state = ActivationJournalState.TESTING_HEALTH))
        val connRes = tunnelController.connect()
        if (connRes is TunnelResult.Failure) {
            return rollback(journal, "Tunnel connection failed: ${connRes.message}")
        }

        // 3. Check health
        val isHealthy = healthProbe.checkHealth()
        if (!isHealthy) {
            return rollback(journal, "Tunnel health check failed after connection")
        }

        // 4. Commit candidate as new Active / LKG
        secureStorage.setString("active_version", candidateVersion.toString())
        secureStorage.setString("last_known_good_version", candidateVersion.toString())
        saveJournal(journal.copy(state = ActivationJournalState.COMMITTED))

        return ActivationOutcome.Success(candidateVersion)
    }

    private suspend fun rollback(journal: ActivationJournalEntry, error: String): ActivationOutcome {
        saveJournal(journal.copy(state = ActivationJournalState.ROLLING_BACK, errorMessage = error))

        // Disconnect failed candidate
        tunnelController.disconnect("Rollback due to failure: $error")

        val lkgVersion = journal.lastKnownGoodVersion
        if (lkgVersion != null) {
            // Restore LKG active profile
            secureStorage.setString("active_version", lkgVersion.toString())
            saveJournal(journal.copy(state = ActivationJournalState.ROLLED_BACK))
            return ActivationOutcome.RolledBack(lkgVersion, error)
        } else {
            // No prior version to rollback to
            secureStorage.remove("active_version")
            saveJournal(journal.copy(state = ActivationJournalState.ROLLED_BACK))
            return ActivationOutcome.RolledBack(null, "$error (no previous version available)")
        }
    }
}
