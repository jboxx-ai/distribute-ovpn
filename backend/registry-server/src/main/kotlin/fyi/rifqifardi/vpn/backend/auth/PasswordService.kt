package fyi.rifqifardi.vpn.backend.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object PasswordService {

    fun generateSalt(length: Int = 16): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(length)
        random.nextBytes(saltBytes)
        return Base64.getEncoder().encodeToString(saltBytes)
    }

    fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(Base64.getDecoder().decode(salt))
        val hashed = md.digest(password.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashed)
    }

    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        val calculated = hashPassword(password, salt)
        return MessageDigest.isEqual(
            calculated.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }
}
