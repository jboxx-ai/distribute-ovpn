# Design Document: Android Client UI Architecture (Kumo Design Language)

## 1. Design System & Theme Mapping (Jetpack Compose)

Cloudflare Kumo UI tokens are mapped into a native `KumoTheme` Composable structure:

### 1.1 Compose Color Palette
```kotlin
object KumoColors {
    val Base = Color(0xFF0D1117)          // Dark surface canvas
    val Elevated = Color(0xFF161B22)      // Card & sheet backgrounds
    val Recessed = Color(0xFF010409)      // Code containers & logs
    val Line = Color(0xFF30363D)          // Borders & dividers
    val Brand = Color(0xFFF38020)         // Cloudflare Orange Accent
    
    // Status Semantics
    val Success = Color(0xFF3FB950)       // Connected / Committed
    val SuccessSubtle = Color(0x1F3FB950)
    val Warning = Color(0xFFD29922)       // Staging / Testing Health
    val WarningSubtle = Color(0x1FD29922)
    val Danger = Color(0xFFF85149)        // Disconnected / Failed / Rolled Back
    val DangerSubtle = Color(0x1FF85149)
}
```

---

## 2. Screen Hierarchy & Navigation Graph

```text
NavigationHost
├── Screen.Main (Tunnel Dashboard)
│   ├── State: DISCONNECTED -> Action: [Connect]
│   ├── State: CONNECTING / ACTIVATING -> Action: [Cancel] + Spinner
│   └── State: CONNECTED -> Action: [Disconnect] + Live Uptime & IP
├── Screen.Enrollment (Bottom Sheet / Full Screen)
│   ├── Token Input Field (with paste & QR scanner button)
│   └── Server Public Key Fingerprint Confirmation Card
├── Screen.JournalHistory (Activation Journal Drawer)
│   └── Timeline of recent profile staging, health checks, and rollbacks
└── Screen.Diagnostics (Support & Logs)
    └── Filterable logcat stream with secret-redaction masking
```

---

## 3. Screen Specifications & Behaviors

### 3.1 Main Connection Dashboard (`TunnelMainScreen`)
- **Hero Status Indicator**:
  - Circular animated status ring using `KumoColors.Brand` when connecting and `KumoColors.Success` when connected.
  - Large label: `CONNECTED`, `CONNECTING`, or `DISCONNECTED`.
- **Profile Metadata Card (`bg-kumo-elevated`)**:
  - Displays Assigned Profile Name (`corp-vpn`), Active Version (`v3`), and Release Channel Badge (`STABLE`).
  - LKG Indicator: Shows Last-Known-Good version fallback status.
- **Connection Button**: High-contrast, full-width button with haptic feedback.

### 3.2 Enrollment & Trust Onboarding (`EnrollmentSheet`)
- **Single-Use Token Entry**:
  - Monospace text field formatted for UUID / token strings.
  - "Scan QR" button launching camera barcode reader.
- **Verification Card**: Displays the server's Ed25519 public key fingerprint (SHA-256) for manual out-of-band verification before saving credentials to Android Keystore.

### 3.3 Activation Journal & Rollback Monitor (`JournalDialog`)
- Visualizes the 7-state transactional flow during profile updates:
  - `IDLE` $\rightarrow$ `STAGING` $\rightarrow$ `ACTIVATING` $\rightarrow$ `TESTING_HEALTH` $\rightarrow$ `COMMITTED`
- In case of a bad profile rollout, displays a clear warning card:  
  *"Candidate configuration failed health verification. Automatically rolled back to Last-Known-Good (v2)."*
