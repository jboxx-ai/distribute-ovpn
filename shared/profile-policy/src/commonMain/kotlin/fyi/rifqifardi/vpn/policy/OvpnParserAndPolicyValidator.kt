package fyi.rifqifardi.vpn.policy

data class ParsedOvpnConfig(
    val directives: Map<String, List<String>>,
    val inlineBlocks: Map<String, String>,
    val remotes: List<RemoteEndpoint>
)

data class RemoteEndpoint(
    val host: String,
    val port: Int,
    val protocol: String
)

sealed class PolicyValidationResult {
    data class Valid(val parsed: ParsedOvpnConfig) : PolicyValidationResult()
    data class Invalid(val errors: List<String>) : PolicyValidationResult()
}

class OvpnParserAndPolicyValidator {

    private val forbiddenDirectives = setOf(
        "script-security",
        "up",
        "down",
        "route-up",
        "ipchange",
        "client-connect",
        "client-disconnect",
        "auth-user-pass-verify",
        "tls-verify",
        "plugin"
    )

    private val requiredDirectives = setOf(
        "client",
        "dev",
        "proto"
    )

    fun parse(ovpnContent: String): ParsedOvpnConfig {
        val directives = mutableMapOf<String, MutableList<String>>()
        val inlineBlocks = mutableMapOf<String, String>()
        val remotes = mutableListOf<RemoteEndpoint>()

        val lines = ovpnContent.lines()
        var currentBlockTag: String? = null
        val currentBlockContent = StringBuilder()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                continue
            }

            if (currentBlockTag != null) {
                if (line == "</$currentBlockTag>") {
                    inlineBlocks[currentBlockTag] = currentBlockContent.toString().trim()
                    currentBlockTag = null
                    currentBlockContent.clear()
                } else {
                    currentBlockContent.append(rawLine).append("\n")
                }
                continue
            }

            if (line.startsWith("<") && line.endsWith(">")) {
                currentBlockTag = line.substring(1, line.length - 1)
                continue
            }

            val parts = line.split("\\s+".toRegex())
            val key = parts[0].lowercase()
            val value = parts.drop(1).joinToString(" ")

            directives.getOrPut(key) { mutableListOf() }.add(value)

            if (key == "remote") {
                val host = parts.getOrNull(1) ?: ""
                val port = parts.getOrNull(2)?.toIntOrNull() ?: 1194
                val proto = parts.getOrNull(3)?.lowercase() ?: "udp"
                remotes.add(RemoteEndpoint(host, port, proto))
            }
        }

        return ParsedOvpnConfig(
            directives = directives,
            inlineBlocks = inlineBlocks,
            remotes = remotes
        )
    }

    fun validate(ovpnContent: String): PolicyValidationResult {
        val parsed = parse(ovpnContent)
        val errors = mutableListOf<String>()

        for (forbidden in forbiddenDirectives) {
            if (parsed.directives.containsKey(forbidden)) {
                errors.add("Forbidden directive detected for remote distribution: '$forbidden'")
            }
        }

        for (required in requiredDirectives) {
            if (!parsed.directives.containsKey(required)) {
                errors.add("Missing required client directive: '$required'")
            }
        }

        if (parsed.remotes.isEmpty() && !parsed.directives.containsKey("remote")) {
            errors.add("No 'remote' server endpoint specified in configuration")
        }

        if (!parsed.inlineBlocks.containsKey("ca") && !parsed.directives.containsKey("ca")) {
            errors.add("Missing CA certificate directive or <ca> inline block")
        }

        return if (errors.isEmpty()) {
            PolicyValidationResult.Valid(parsed)
        } else {
            PolicyValidationResult.Invalid(errors)
        }
    }
}
