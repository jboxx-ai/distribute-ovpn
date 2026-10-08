# OpenSpec UI Design Systems: Cloudflare Kumo UI Integration

This document outlines the UI architecture, routing maps, design tokens, and component specifications across all three client and operator surfaces for the OpenSpec KMP VPN Platform.

---

## 🎨 Global Design System: Cloudflare Kumo UI

All surfaces share the same visual DNA and semantic tokens inspired by the **Cloudflare Kumo UI** design language:

```
┌──────────────────────────────────────────────────────────────┐
│                    Cloudflare Kumo Tokens                    │
├──────────────────────────────┬───────────────────────────────┤
│ Surface Hierarchy            │ Status Semantics              │
│ • bg-kumo-base: #0D1117       │ • text-kumo-success: #3FB950  │
│ • bg-kumo-elevated: #161B22  │ • text-kumo-warning: #D29922  │
│ • bg-kumo-recessed: #010409  │ • text-kumo-danger: #F85149   │
│ • border-kumo-line: #30363D  │ • bg-kumo-brand: #F38020      │
└──────────────────────────────┴───────────────────────────────┘
```

---

## 🖥️ 1. Web Console Profile Registry (`phase-08-ui-console-registry`)

- **Tech Stack**: React 19, TypeScript, Vite, Tailwind CSS v4, Base UI.
- **Specification**: [`openspec/changes/phase-08-ui-console-registry/design.md`](../../openspec/changes/phase-08-ui-console-registry/design.md)
- **Primary Routes**:
  - `/overview`: High-density operator dashboard with metric cards and recent audit trails.
  - `/profiles`: Versioned profile catalog with `.ovpn` AST policy inspector and channel promoter.
  - `/devices`: Device trust table, single-use token generator modal, and instant revocation.
  - `/audit`: Tamper-evident operator activity viewer with secret-redaction filters.

---

## 📱 2. Android Client Application (`phase-09-ui-android-client`)

- **Tech Stack**: Kotlin, Jetpack Compose, Material 3 + KumoTheme.
- **Specification**: [`openspec/changes/phase-09-ui-android-client/design.md`](../../openspec/changes/phase-09-ui-android-client/design.md)
- **Primary Screens**:
  - `TunnelMainScreen`: Hero connection status ring, active profile card, and one-tap connect button.
  - `EnrollmentSheet`: Single-use token input, QR scanner, and Ed25519 server fingerprint verification.
  - `JournalDialog`: 7-state transactional sync step-by-step viewer with auto-rollback alerts.
  - `DiagnosticsSheet`: Redacted local logcat bundle generator for support.

---

## 🍏 3. macOS Client Application (`phase-10-ui-macos-client`)

- **Tech Stack**: Swift 6, SwiftUI, macOS MenuBarExtra, Apple NetworkExtension.
- **Specification**: [`openspec/changes/phase-10-ui-macos-client/design.md`](../../openspec/changes/phase-10-ui-macos-client/design.md)
- **Primary Surfaces**:
  - `MenuBarExtra`: Lightweight status bar companion with quick connect toggle and status pill.
  - `MainWindow`: Full `NavigationSplitView` dashboard with latency charts, version timeline, and sync triggers.
  - `KeychainEnrollment`: macOS Keychain native credential storage flow with security validation.
