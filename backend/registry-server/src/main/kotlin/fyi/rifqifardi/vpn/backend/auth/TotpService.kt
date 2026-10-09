package fyi.rifqifardi.vpn.backend.auth

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TotpService {

    private const val TIME_STEP_SECONDS = 30L
    private const val TOTP_DIGITS = 6
    private const val DIGITS_POWER = 1_000_000 // 10^6
    private const val BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun generateSecret(byteLength: Int = 20): String {
        val random = SecureRandom()
        val bytes = ByteArray(byteLength)
        random.nextBytes(bytes)
        return encodeBase32(bytes)
    }

    fun generateOtpUri(username: String, secret: String, issuer: String = "OpenSpecVPN"): String {
        return "otpauth://totp/$issuer:$username?secret=$secret&issuer=$issuer&algorithm=SHA1&digits=$TOTP_DIGITS&period=$TIME_STEP_SECONDS"
    }

    fun generateCurrentCode(secret: String, timestampMs: Long = System.currentTimeMillis()): String {
        val timeStep = (timestampMs / 1000L) / TIME_STEP_SECONDS
        return generateCode(secret, timeStep)
    }

    fun verifyCode(
        secret: String,
        code: String,
        timestampMs: Long = System.currentTimeMillis(),
        windowSteps: Int = 1
    ): Boolean {
        val trimmed = code.trim()
        if (trimmed.length != TOTP_DIGITS) return false

        val currentStep = (timestampMs / 1000L) / TIME_STEP_SECONDS
        for (i in -windowSteps..windowSteps) {
            val step = currentStep + i
            val expectedCode = generateCode(secret, step)
            if (expectedCode == trimmed) {
                return true
            }
        }
        return false
    }

    private fun generateCode(secret: String, timeStep: Long): String {
        val keyBytes = decodeBase32(secret)
        val data = ByteBuffer.allocate(8).putLong(timeStep).array()

        val mac = Mac.getInstance("HmacSHA1")
        val signKey = SecretKeySpec(keyBytes, "HmacSHA1")
        mac.init(signKey)
        val hash = mac.doFinal(data)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val otp = binary % DIGITS_POWER
        return otp.toString().padStart(TOTP_DIGITS, '0')
    }

    private fun encodeBase32(data: ByteArray): String {
        val sb = StringBuilder()
        var buffer = 0
        var bufferLength = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bufferLength += 8
            while (bufferLength >= 5) {
                val index = (buffer shr (bufferLength - 5)) and 0x1F
                sb.append(BASE32_CHARS[index])
                bufferLength -= 5
            }
        }
        if (bufferLength > 0) {
            val index = (buffer shl (5 - bufferLength)) and 0x1F
            sb.append(BASE32_CHARS[index])
        }
        return sb.toString()
    }

    private fun decodeBase32(base32: String): ByteArray {
        val clean = base32.uppercase().replace("=", "").trim()
        val out = mutableListOf<Byte>()
        var buffer = 0
        var bufferLength = 0

        for (c in clean) {
            val charIndex = BASE32_CHARS.indexOf(c)
            if (charIndex == -1) continue
            buffer = (buffer shl 5) or charIndex
            bufferLength += 5
            if (bufferLength >= 8) {
                val b = (buffer shr (bufferLength - 8)) and 0xFF
                out.add(b.toByte())
                bufferLength -= 8
            }
        }
        return out.toByteArray()
    }
}
