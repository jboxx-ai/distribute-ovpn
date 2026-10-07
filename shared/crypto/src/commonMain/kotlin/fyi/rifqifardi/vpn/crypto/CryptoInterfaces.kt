package fyi.rifqifardi.vpn.crypto

import fyi.rifqifardi.vpn.model.ProfileDigest
import fyi.rifqifardi.vpn.model.ProfileSignature

interface SignatureVerifier {
    suspend fun verify(contentBytes: ByteArray, signature: ProfileSignature, publicKeyBase64: String): Boolean
}

interface DigestCalculator {
    fun calculateSha256(contentBytes: ByteArray): ProfileDigest
}
