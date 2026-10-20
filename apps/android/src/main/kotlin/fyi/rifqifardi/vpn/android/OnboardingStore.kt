package fyi.rifqifardi.vpn.android

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import javax.crypto.Cipher

/**
 * MVP setup metadata store. Device credentials are RSA-OAEP wrapped to a non-exportable
 * Android Keystore key before SharedPreferences persistence. This is credential-at-rest
 * protection, not proof of a device-bound Ed25519 enrollment identity.
 */
internal class OnboardingStore(context: Context) {
    private val prefs = context.getSharedPreferences("vpn_distribution_setup", Context.MODE_PRIVATE)

    var registryUrl: String?
        get() = prefs.getString(KEY_REGISTRY_URL, null)
        set(value) { prefs.edit().putString(KEY_REGISTRY_URL, value).apply() }

    val deviceId: String? get() = prefs.getString(KEY_DEVICE_ID, null)
    val deviceToken: String? get() = prefs.getString(KEY_DEVICE_TOKEN, null)?.let { encoded ->
        runCatching {
            val store = keyStore()
            val privateKey = store.getKey(KEY_ALIAS, null)
            Cipher.getInstance(TRANSFORMATION).run {
                init(Cipher.DECRYPT_MODE, privateKey)
                String(doFinal(Base64.decode(encoded, Base64.NO_WRAP)), Charsets.UTF_8)
            }
        }.getOrElse {
            clearEnrollment()
            null
        }
    }
    val signingPublicKey: String? get() = prefs.getString(KEY_SIGNING_KEY, null)

    fun saveEnrollment(deviceId: String, deviceToken: String, signingPublicKey: String) {
        require(deviceId.isNotBlank() && deviceToken.isNotBlank() && signingPublicKey.isNotBlank())
        val store = keyStore()
        if (!store.containsAlias(KEY_ALIAS)) {
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore").apply {
                initialize(
                    KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setKeySize(2048)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                        .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                        .setRandomizedEncryptionRequired(true)
                        .build()
                )
            }.generateKeyPair()
        }
        val publicKey = store.getCertificate(KEY_ALIAS).publicKey
        val wrapped = Cipher.getInstance(TRANSFORMATION).run {
            init(Cipher.ENCRYPT_MODE, publicKey)
            doFinal(deviceToken.toByteArray(Charsets.UTF_8))
        }
        prefs.edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_DEVICE_TOKEN, Base64.encodeToString(wrapped, Base64.NO_WRAP))
            .putString(KEY_SIGNING_KEY, signingPublicKey)
            .commit()
    }

    fun clearEnrollment() {
        prefs.edit().remove(KEY_DEVICE_ID).remove(KEY_DEVICE_TOKEN).remove(KEY_SIGNING_KEY).commit()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    companion object {
        private const val KEY_REGISTRY_URL = "registry_url"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_TOKEN = "device_token_wrapped"
        private const val KEY_SIGNING_KEY = "server_signing_public_key"
        private const val KEY_ALIAS = "vpn_distribution_device_token_wrap"
        private const val TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    }
}
