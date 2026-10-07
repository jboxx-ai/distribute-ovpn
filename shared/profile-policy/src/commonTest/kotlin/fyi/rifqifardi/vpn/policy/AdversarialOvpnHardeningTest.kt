package fyi.rifqifardi.vpn.policy

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AdversarialOvpnHardeningTest {

    private val validator = OvpnParserAndPolicyValidator()

    @Test
    fun testRejectLeadingWhitespaceDirectives() {
        val payload = """
            client
            dev tun
            proto udp
            remote 1.2.3.4 1194
                script-security   3
            up "/tmp/evil.sh"
            <ca>cert</ca>
        """.trimIndent()

        val res = validator.validate(payload)
        assertIs<PolicyValidationResult.Invalid>(res)
        assertTrue(res.errors.any { it.contains("script-security") })
        assertTrue(res.errors.any { it.contains("up") })
    }

    @Test
    fun testRejectMixedCaseDirectives() {
        val payload = """
            CLIENT
            DEV tun
            PROTO udp
            REMOTE 1.2.3.4 1194
            ScRiPt-SeCuRiTy 2
            <ca>cert</ca>
        """.trimIndent()

        val res = validator.validate(payload)
        assertIs<PolicyValidationResult.Invalid>(res)
        assertTrue(res.errors.any { it.contains("script-security") })
    }

    @Test
    fun testRejectPluginDirectives() {
        val payload = """
            client
            dev tun
            proto udp
            remote 1.2.3.4 1194
            plugin /usr/lib/openvpn/openvpn-auth-pam.so
            <ca>cert</ca>
        """.trimIndent()

        val res = validator.validate(payload)
        assertIs<PolicyValidationResult.Invalid>(res)
        assertTrue(res.errors.any { it.contains("plugin") })
    }

    @Test
    fun testRejectInlineScriptBlocks() {
        val payload = """
            client
            dev tun
            proto udp
            remote 1.2.3.4 1194
            down /bin/sh
            <ca>cert</ca>
        """.trimIndent()

        val res = validator.validate(payload)
        assertIs<PolicyValidationResult.Invalid>(res)
        assertTrue(res.errors.any { it.contains("down") })
    }
}
