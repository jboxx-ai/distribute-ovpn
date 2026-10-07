package fyi.rifqifardi.vpn.sync.activation

import fyi.rifqifardi.vpn.domain.contracts.TunnelController
import fyi.rifqifardi.vpn.domain.contracts.TunnelResult
import fyi.rifqifardi.vpn.domain.contracts.ValidatedProfile
import fyi.rifqifardi.vpn.model.*
import fyi.rifqifardi.vpn.persistence.InMemoryFileStore
import fyi.rifqifardi.vpn.persistence.InMemorySecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MockTunnelController(var shouldFailConnect: Boolean = false) : TunnelController {
    private val _state = MutableStateFlow(TunnelState.DISCONNECTED)
    override val state: StateFlow<TunnelState> = _state.asStateFlow()

    override suspend fun prepare(profile: ValidatedProfile): TunnelResult = TunnelResult.Success

    override suspend fun connect(): TunnelResult {
        return if (shouldFailConnect) {
            _state.value = TunnelState.FAILED
            TunnelResult.Failure("Simulated tunnel handshake timeout")
        } else {
            _state.value = TunnelState.CONNECTED
            TunnelResult.Success
        }
    }

    override suspend fun disconnect(reason: String): TunnelResult {
        _state.value = TunnelState.DISCONNECTED
        return TunnelResult.Success
    }
}

class SafeActivationEngineTest {

    private fun createSampleProfile(version: Long): ValidatedProfile {
        return ValidatedProfile(
            manifest = ProfileManifest(
                schemaVersion = 1,
                profileId = "corp-vpn",
                version = version,
                channel = ReleaseChannel.STABLE,
                digest = ProfileDigest(algorithm = "SHA-256", hexValue = "abc"),
                signature = ProfileSignature(algorithm = "Ed25519", keyId = "k1", signatureBase64 = "sig"),
                downloadUrl = "http://localhost/art",
                createdAtEpochMs = 1000L
            ),
            ovpnConfigContent = "client\ndev tun\nremote 1.2.3.4 1194\n<ca>cert</ca>",
            validatedAtEpochMs = 1000L
        )
    }

    @Test
    fun testSuccessfulActivation() = runTest {
        val storage = InMemorySecureStorage()
        val fileStore = InMemoryFileStore()
        val tunnel = MockTunnelController(shouldFailConnect = false)
        val engine = SafeActivationEngine(storage, fileStore, tunnel)

        val profile = createSampleProfile(1)
        val outcome = engine.activate(profile)

        assertIs<ActivationOutcome.Success>(outcome)
        assertEquals(1L, outcome.version)
        assertEquals("1", storage.getString("active_version"))
        assertEquals("1", storage.getString("last_known_good_version"))
    }

    @Test
    fun testAutomaticRollbackOnFailure() = runTest {
        val storage = InMemorySecureStorage()
        val fileStore = InMemoryFileStore()
        val tunnel = MockTunnelController(shouldFailConnect = false)
        val engine = SafeActivationEngine(storage, fileStore, tunnel)

        // 1. Establish Version 1 as LKG
        engine.activate(createSampleProfile(1))
        assertEquals("1", storage.getString("active_version"))

        // 2. Candidate Version 2 fails during connect
        tunnel.shouldFailConnect = true
        val failedOutcome = engine.activate(createSampleProfile(2))

        assertIs<ActivationOutcome.RolledBack>(failedOutcome)
        assertEquals(1L, failedOutcome.activeVersion)
        assertEquals("1", storage.getString("active_version"), "Must preserve last known good version")

        val journal = engine.getJournal()
        assertEquals(ActivationJournalState.ROLLED_BACK, journal?.state)
    }
}
