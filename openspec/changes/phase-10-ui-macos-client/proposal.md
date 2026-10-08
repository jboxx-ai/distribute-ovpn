# Phase 10 — macOS Client UI Architecture (MenuBar & SwiftUI)

## Why
macOS desktop power users require a zero-friction system menu bar accessory alongside a full-featured management window, styled to match Cloudflare Kumo UI aesthetics and deeply integrated with Apple NetworkExtension and macOS Keychain.

## What Changes
- Implement a dual-surface SwiftUI application: a lightweight `MenuBarExtra` for status & quick actions, and a comprehensive `MainWindow` for deep inspection.
- Translate Cloudflare Kumo UI design tokens (dark surface canvas, orange brand accent, status rings) into native macOS SwiftUI styling.
- Connect UI views to shared KMP state flows and macOS Keychain security APIs.

## Capabilities

### New Capabilities
- `macos-menubar-ui`: Persistent menu bar icon with connection states and quick toggle.
- `macos-window-dashboard`: Full window dashboard with throughput charts, version timeline, and channel picker.
- `macos-keychain-enrollment`: Zero-touch enrollment dialog securely writing client tokens to system Keychain.
- `macos-diagnostics-export`: One-click redacted diagnostic bundle generator.

## Impact
- Delivers a native, high-performance desktop experience adhering strictly to Apple Human Interface Guidelines and Kumo UI styling.
