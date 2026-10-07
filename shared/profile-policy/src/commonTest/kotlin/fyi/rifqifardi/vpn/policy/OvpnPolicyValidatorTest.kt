package fyi.rifqifardi.vpn.policy

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OvpnPolicyValidatorTest {

    private val validator = OvpnParserAndPolicyValidator()

    @Test
    fun testValidConfiguration() {
        val validOvpn = """
            client
            dev tun
            proto udp
            remote vpn.example.com 1194
            resolv-retry infinite
            nobind
            persist-key
            persist-tun
            <ca>
            -----BEGIN CERTIFICATE-----
            MIIB...
            -----END CERTIFICATE-----
            </ca>
        """.trimIndent()

        val result = validator.validate(validOvpn)
        assertIs<PolicyValidationResult.Valid>(result)
        val parsed = result.parsed
        assertTrue(parsed.directives.containsKey("client"))
        assertTrue(parsed.inlineBlocks.containsKey("ca"))
        assertTrue(parsed.remotes.size == 1)
    }

    @Test
    fun testForbiddenScriptDirectivesRejected() {
        val dangerousOvpn = """
            client
            dev tun
            proto udp
            remote vpn.example.com 1194
            script-security 2
            up /etc/openvpn/update-resolv-conf
            <ca>
            -----BEGIN CERTIFICATE-----
            MIIB...
            -----END CERTIFICATE-----
            </ca>
        """.trimIndent()

        val result = validator.validate(dangerousOvpn)
        assertIs<PolicyValidationResult.Invalid>(result)
        val errors = (result as PolicyValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("script-security") })
        assertTrue(errors.any { it.contains("up") })
    }

    @Test
    fun testMissingRequiredDirectivesRejected() {
        val incompleteOvpn = """
            dev tun
            proto udp
        """.trimIndent()

        val result = validator.validate(incompleteOvpn)
        assertIs<PolicyValidationResult.Invalid>(result)
        val errors = (result as PolicyValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("client") })
        assertTrue(errors.any { it.contains("remote") })
        assertTrue(errors.any { it.contains("ca") })
    }
}
