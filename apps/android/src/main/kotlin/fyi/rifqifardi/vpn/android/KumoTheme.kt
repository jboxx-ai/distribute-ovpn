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
    // Explicit Int ARGB values: GradientDrawable.setColor accepts a signed 32-bit color.
    // Without the type annotation, opaque literals above Int.MAX_VALUE are Longs; keeping
    // the conversion at the token boundary prevents an accidental widened/invalid value
    // from reaching imperative View backgrounds.
    const val Base: Int = 0xFF0D1117.toInt()          // Dark surface canvas
    const val Elevated: Int = 0xFF161B22.toInt()      // Card & sheet backgrounds
    const val Recessed: Int = 0xFF010409.toInt()      // Code containers, terminal & logs
    const val Line: Int = 0xFF30363D.toInt()          // Borders & structural dividers
    const val Brand: Int = 0xFFF38020.toInt()         // Cloudflare Orange Accent
    const val PrimaryText: Int = 0xFFE6EDF3.toInt()   // High-contrast primary text
    const val MutedText: Int = 0xFF8B949E.toInt()     // Secondary / meta text

    // Status Semantics
    const val Success: Int = 0xFF3FB950.toInt()      // Connected / Committed / Valid
    const val SuccessSubtle: Int = 0x1F3FB950.toInt()
    const val Warning: Int = 0xFFD29922.toInt()       // Staging / Beta / Health Warning
    const val WarningSubtle: Int = 0x1FD29922.toInt()
    const val Danger: Int = 0xFFF85149.toInt()        // Disconnected / Failed / Revoked
    const val DangerSubtle: Int = 0x1FF85149.toInt()
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
