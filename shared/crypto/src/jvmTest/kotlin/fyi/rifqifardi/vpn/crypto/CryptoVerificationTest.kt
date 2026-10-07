package fyi.rifqifardi.vpn.crypto

import fyi.rifqifardi.vpn.model.ProfileSignature
import kotlinx.coroutines.test.runTest
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CryptoVerificationTest {

    private val verifier = Ed25519SignatureVerifier()
    private val digestCalculator = CommonDigestCalculator()

    @Test
    fun testValidEd25519SignatureVerification() = runTest {
        val kpg = KeyPairGenerator.getInstance("Ed25519")
        val keyPair = kpg.generateKeyPair()

        val publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)
        val testPayload = "client\ndev tun\nremote 1.2.3.4 1194".toByteArray()

        val signer = Signature.getInstance("Ed25519")
        signer.initSign(keyPair.private)
        signer.update(testPayload)
        val signatureBytes = signer.sign()
        val signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes)

        val sigObj = ProfileSignature(algorithm = "Ed25519", keyId = "test-key-1", signatureBase64 = signatureBase64)

        // Positive verification
        val isValid = verifier.verify(testPayload, sigObj, publicKeyBase64)
        assertTrue(isValid, "Expected valid signature to verify successfully")

        // Tampered payload verification
        val tamperedPayload = "client\ndev tun\nremote 1.2.3.5 1194".toByteArray()
        val isTamperedValid = verifier.verify(tamperedPayload, sigObj, publicKeyBase64)
        assertFalse(isTamperedValid, "Expected tampered payload to fail verification")

        // Wrong key verification
        val anotherKeyPair = kpg.generateKeyPair()
        val wrongPublicKey = Base64.getEncoder().encodeToString(anotherKeyPair.public.encoded)
        val isWrongKeyValid = verifier.verify(testPayload, sigObj, wrongPublicKey)
        assertFalse(isWrongKeyValid, "Expected verification with wrong key to fail")
    }

    @Test
    fun testSha256DigestCalculation() {
        val payload = "hello world".toByteArray()
        val digest = digestCalculator.calculateSha256(payload)
        // b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9
        assertTrue(digest.hexValue.isNotEmpty())
        assertTrue(digest.hexValue == "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9")
    }
}
