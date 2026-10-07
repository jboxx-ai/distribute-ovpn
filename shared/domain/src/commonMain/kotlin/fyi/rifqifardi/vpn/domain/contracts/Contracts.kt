package fyi.rifqifardi.vpn.domain.contracts

import fyi.rifqifardi.vpn.model.ProfileManifest
import fyi.rifqifardi.vpn.model.TunnelState
import kotlinx.coroutines.flow.StateFlow

data class ValidatedProfile(
    val manifest: ProfileManifest,
    val ovpnConfigContent: String,
    val validatedAtEpochMs: Long
)

sealed class TunnelResult {
    data object Success : TunnelResult()
    data class Failure(val message: String, val cause: Throwable? = null) : TunnelResult()
}

interface TunnelController {
    val state: StateFlow<TunnelState>
    suspend fun prepare(profile: ValidatedProfile): TunnelResult
    suspend fun connect(): TunnelResult
    suspend fun disconnect(reason: String): TunnelResult
}

interface SecureStorage {
    suspend fun getString(key: String): String?
    suspend fun setString(key: String, value: String)
    suspend fun remove(key: String)
    suspend fun clear()
}

interface FileStore {
    suspend fun writeBytes(path: String, bytes: ByteArray)
    suspend fun readBytes(path: String): ByteArray?
    suspend fun delete(path: String): Boolean
    suspend fun exists(path: String): Boolean
}

interface Clock {
    fun nowEpochMs(): Long
}

class SystemClock : Clock {
    override fun nowEpochMs(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
}

interface TelemetrySink {
    suspend fun recordEvent(name: String, attributes: Map<String, String>)
    suspend fun recordError(message: String, throwable: Throwable?)
}
