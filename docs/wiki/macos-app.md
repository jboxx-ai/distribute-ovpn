# macOS Client Application

## 1. Subsystem Overview
The macOS client application (`:apps:macos`) provides a native SwiftUI application integrated with macOS `NetworkExtension` framework and the shared Kotlin/Native `VpnCore.framework`.

---

## 2. Architecture & Umbrella Integration

```text
┌────────────────────────────────────────────────────────┐
│                   macOS UI (SwiftUI)                   │
│   - ContentView.swift                                  │
│   - VpnMacOSApp.swift                                  │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│           MacOSTunnelManager (ObservableObject)        │
│   - Binds KMP TunnelState to Combine / SwiftUI         │
│   - Controls NETunnelProviderManager IPC               │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│        Kotlin/Native Shared Umbrella Framework         │
│                 (VpnCore.framework)                    │
│   - Shared Activation Journal & Sync Engine            │
│   - OvpnParserAndPolicyValidator                       │
│   - Cryptographic Signature Verification               │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│     App Extension: PacketTunnelProvider.swift          │
│   - NEPacketTunnelProvider implementation              │
│   - Configures NEPacketTunnelNetworkSettings (IP/DNS)   │
└────────────────────────────────────────────────────────┘
```

---

## 3. Security & Sandboxing (macOS)
- **Keychain**: Device credentials stored under macOS Keychain with Access Control Lists (ACL).
- **NetworkExtension Entitlements**: Requires `com.apple.developer.networking.networkextension` with `packet-tunnel-provider`.
- **Hardened Runtime & Notarization**: Code signed with Developer ID Application certificate and notarized via Apple Notary Service.
