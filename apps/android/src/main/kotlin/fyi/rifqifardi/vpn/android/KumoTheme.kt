package fyi.rifqifardi.vpn.android

/**
 * Cloudflare Kumo UI Semantic Color Tokens & Theme Definition for Android (Compose / Design System).
 *
 * Implements OpenSpec Phase 09 UI contract:
 * - Canvas base, elevated card, and recessed container surfaces.
 * - Semantic status colors (Success, Warning, Danger).
 * - Cloudflare Orange brand accent.
 */
object KumoColors {
    // 32-bit ARGB Longs / Ints for native rendering compatibility
    const val Base = 0xFF0D1117          // Dark surface canvas
    const val Elevated = 0xFF161B22      // Card & sheet backgrounds
    const val Recessed = 0xFF010409      // Code containers, terminal & logs
    const val Line = 0xFF30363D          // Borders & structural dividers
    const val Brand = 0xFFF38020         // Cloudflare Orange Accent
    const val PrimaryText = 0xFFE6EDF3   // High-contrast primary text
    const val MutedText = 0xFF8B949E     // Secondary / meta text

    // Status Semantics
    const val Success = 0xFF3FB950       // Connected / Committed / Valid
    const val SuccessSubtle = 0x1F3FB950
    const val Warning = 0xFFD29922       // Staging / Beta / Health Warning
    const val WarningSubtle = 0x1FD29922
    const val Danger = 0xFFF85149        // Disconnected / Failed / Revoked
    const val DangerSubtle = 0x1FF85149
}

enum class KumoStatus {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
    STAGED,
    REVOKED
}

data class KumoProfileUiModel(
    val id: String,
    val name: String,
    val activeVersion: Int,
    val channel: String,
    val lastKnownGoodVersion: Int,
    val digest: String
)
