package fyi.rifqifardi.vpn.backend

import fyi.rifqifardi.vpn.backend.auth.TotpService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TotpAuthenticationTest {

    @Test
    fun testTotpGenerationAndValidation() {
        val secret = TotpService.generateSecret()
        val now = System.currentTimeMillis()

        // 1. Generate current code
        val code = TotpService.generateCurrentCode(secret, now)
        assertEquals(6, code.length)
        assertTrue(code.all { it.isDigit() })

        // 2. Verify current code matches
        val isValid = TotpService.verifyCode(secret, code, now)
        assertTrue(isValid, "Generated code should verify successfully at current time")

        // 3. Verify invalid code is rejected
        val isInvalidCode = TotpService.verifyCode(secret, "000000", now)
        if (code != "000000") {
            assertFalse(isInvalidCode, "Invalid code should be rejected")
        }

        // 4. Verify wrong secret is rejected
        val differentSecret = TotpService.generateSecret()
        val isWrongSecret = TotpService.verifyCode(differentSecret, code, now)
        assertFalse(isWrongSecret, "Code from different secret should be rejected")
    }

    @Test
    fun testTotpTimeDriftToleranceWindow() {
        val secret = TotpService.generateSecret()
        val now = 1_700_000_000_000L // arbitrary fixed epoch ms

        val currentCode = TotpService.generateCurrentCode(secret, now)

        // Previous window (-30s)
        val pastTime = now - 30_000L
        val pastCode = TotpService.generateCurrentCode(secret, pastTime)
        assertTrue(
            TotpService.verifyCode(secret, pastCode, now, windowSteps = 1),
            "Code from previous 30s step should verify within ±1 window"
        )

        // Future window (+30s)
        val futureTime = now + 30_000L
        val futureCode = TotpService.generateCurrentCode(secret, futureTime)
        assertTrue(
            TotpService.verifyCode(secret, futureCode, now, windowSteps = 1),
            "Code from next 30s step should verify within ±1 window"
        )

        // Out of window (-90s)
        val expiredTime = now - 90_000L
        val expiredCode = TotpService.generateCurrentCode(secret, expiredTime)
        assertFalse(
            TotpService.verifyCode(secret, expiredCode, now, windowSteps = 1),
            "Code older than 60s should be rejected"
        )
    }

    @Test
    fun testOtpUriFormatting() {
        val secret = "JBSWY3DPEHPK3PXP"
        val uri = TotpService.generateOtpUri("admin", secret, "OpenSpecVPN")
        assertTrue(uri.startsWith("otpauth://totp/OpenSpecVPN:admin"))
        assertTrue(uri.contains("secret=JBSWY3DPEHPK3PXP"))
        assertTrue(uri.contains("issuer=OpenSpecVPN"))
    }
}
