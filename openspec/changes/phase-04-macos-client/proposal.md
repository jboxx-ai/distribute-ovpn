# Phase 04 — macOS Managed OpenVPN Client MVP

## Why

The platform needs a macOS client sharing the same registry, enrollment, validation, and synchronization semantics while using Apple's required NetworkExtension execution model.

## What Changes

- Create SwiftUI macOS host application consuming the KMP framework.
- Implement macOS enrollment and Keychain-backed device identity.
- Implement profile synchronization using the shared KMP engine.
- Implement `NEPacketTunnelProvider` VPN extension.
- Integrate native OpenVPN core through a narrow bridge.
- Report status using the same cross-platform contract.

## Capabilities

### New Capabilities

- `macos-enrollment`
- `macos-profile-sync`
- `macos-openvpn-tunnel`
- `macos-client-status`

## Impact

- Introduces Apple signing, entitlements, and NetworkExtension provisioning as build dependencies.
- Establishes cross-platform behavioral parity with Android.
