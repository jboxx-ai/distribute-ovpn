package fyi.rifqifardi.vpn.persistence

import fyi.rifqifardi.vpn.domain.contracts.FileStore
import fyi.rifqifardi.vpn.domain.contracts.SecureStorage

class InMemorySecureStorage : SecureStorage {
    private val store = mutableMapOf<String, String>()

    override suspend fun getString(key: String): String? = store[key]

    override suspend fun setString(key: String, value: String) {
        store[key] = value
    }

    override suspend fun remove(key: String) {
        store.remove(key)
    }

    override suspend fun clear() {
        store.clear()
    }
}

class InMemoryFileStore : FileStore {
    private val files = mutableMapOf<String, ByteArray>()

    override suspend fun writeBytes(path: String, bytes: ByteArray) {
        files[path] = bytes
    }

    override suspend fun readBytes(path: String): ByteArray? = files[path]

    override suspend fun delete(path: String): Boolean = files.remove(path) != null

    override suspend fun exists(path: String): Boolean = files.containsKey(path)
}
