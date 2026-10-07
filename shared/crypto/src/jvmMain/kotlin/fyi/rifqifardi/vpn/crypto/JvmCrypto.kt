package fyi.rifqifardi.vpn.crypto

import fyi.rifqifardi.vpn.model.ProfileDigest
import fyi.rifqifardi.vpn.model.ProfileSignature
import java.security.MessageDigest

class CommonDigestCalculator : DigestCalculator {
    override fun calculateSha256(contentBytes: ByteArray): ProfileDigest {
        val md = MessageDigest.getInstance("SHA-256")
        val hex = md.digest(contentBytes).joinToString("") { "%02x".format(it) }
        return ProfileDigest(algorithm = "SHA-256", hexValue = hex)
    }
}

class Ed25519SignatureVerifier : SignatureVerifier {
    override suspend fun verify(
        contentBytes: ByteArray,
        signature: ProfileSignature,
        publicKeyBase64: String
    ): Boolean {
        if (signature.algorithm != "Ed25519") return false
        return try {
            val keyBytes = java.util.Base64.getDecoder().decode(publicKeyBase64)
            val sigBytes = java.util.Base64.getDecoder().decode(signature.signatureBase64)
            val keySpec = java.security.spec.X509EncodedKeySpec(keyBytes)
            val kf = java.security.KeyFactory.getInstance("Ed25519")
            val pubKey = kf.generatePublic(keySpec)

            val sig = java.security.Signature.getInstance("Ed25519")
            sig.initVerify(pubKey)
            sig.update(contentBytes)
            sig.verify(sigBytes)
        } catch (e: Exception) {
            false
        }
    }
}
