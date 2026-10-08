# Design Document: macOS Client UI Architecture (SwiftUI & Kumo UI)

## 1. Design System & Theme Mapping (SwiftUI)

The macOS client brings Cloudflare Kumo UI's semantic tokens to macOS desktop conventions:

### 1.1 SwiftUI Semantic Color Palette
```swift
extension Color {
    static let kumoBase = Color(nsColor: NSColor(named: "KumoBase") ?? NSColor(red: 0.05, green: 0.07, blue: 0.09, alpha: 1.0))
    static let kumoElevated = Color(nsColor: NSColor(named: "KumoElevated") ?? NSColor(red: 0.09, green: 0.11, blue: 0.13, alpha: 1.0))
    static let kumoRecessed = Color(nsColor: NSColor(named: "KumoRecessed") ?? NSColor(red: 0.01, green: 0.02, blue: 0.04, alpha: 1.0))
    static let kumoLine = Color(nsColor: NSColor(named: "KumoLine") ?? NSColor(red: 0.19, green: 0.21, blue: 0.24, alpha: 1.0))
    static let kumoBrand = Color(red: 0.95, green: 0.50, blue: 0.13) // Cloudflare Orange #F38020

    // Status
    static let kumoSuccess = Color(red: 0.25, green: 0.73, blue: 0.31) // #3FB950
    static let kumoWarning = Color(red: 0.82, green: 0.60, blue: 0.13) // #D29922
    static let kumoDanger = Color(red: 0.97, green: 0.32, blue: 0.29)  // #F85149
}
```

---

## 2. Window & Menu Architecture

```text
macOS Application Surface
├── MenuBarExtra (System Status Bar Item)
│   ├── Header: Connection Status Pill & Active Profile
│   ├── Action: Connect / Disconnect Toggle Button
│   ├── Quick Channel Selector: [STABLE | BETA]
│   ├── Divider
│   ├── "Open Control Panel..." -> Launches MainWindow
│   └── "Quit OpenSpec VPN"
└── MainWindow (NavigationSplitView)
    ├── Sidebar
    │   ├── Overview / Status
    │   ├── Profile & Versions
    │   ├── Activation Journal
    │   ├── Security & Keychain
    │   └── Settings & Diagnostics
    └── Detail View (Selected Tab Content)
```

---

## 3. Surface Specifications & User Behaviors

### 3.1 Menu Bar Accessory (`MenuBarExtra`)
- **Icon States**:
  - `Disconnected`: Subtle outline shield icon.
  - `Connecting`: Pulsing amber activity ring.
  - `Connected`: Solid green shield with lock badge.
- **Popover Menu Content**:
  - Compact summary showing current assigned IP, connection duration, and byte counters.
  - Fast-action toggle switch to activate/deactivate VPN tunnel.

### 3.2 Main Dashboard Window (`MainWindow`)
- **Overview Tab**:
  - High-density telemetry cards (Throughput, Ping / Latency to gateway, Current Profile Digest).
  - Visual timeline showing active configuration version and Last-Known-Good fallback version.
- **Profile & Sync Tab**:
  - View full parsed `.ovpn` configuration with directive syntax highlighting.
  - Sync trigger button to manually query the `:backend:registry-server` for updates.
- **Security & Enrollment Tab**:
  - Token onboarding modal with keychain lock icon.
  - Displays validated Ed25519 server signing key fingerprint.

### 3.3 System Integration & Native Security
- Automatically manages credentials via macOS `Security.framework` (Keychain Access).
- Integrates with `NEPacketTunnelProvider` to route packets via system tunnel device.
