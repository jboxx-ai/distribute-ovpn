# Design — Phase 04

## Process model

```text
macOS Host App (SwiftUI)
    │
    ├── KMP.framework
    │      └── shared enrollment/sync/domain logic
    │
    └── NetworkExtension manager
            │
            ▼
PacketTunnelExtension
    │
    └── native OpenVPN bridge
```

The PacketTunnel extension is a separate execution context. The host app and extension must exchange only the minimum required configuration/state through Apple-supported mechanisms.

## KMP boundary

Export presentation-neutral state and commands from KMP. Keep:
- SwiftUI lifecycle
- NetworkExtension entitlement handling
- app/extension IPC
- Apple logging hooks

in the macOS-specific layer.

## Secure storage

Device authentication secrets live in Keychain. Profile artifacts are stored in an app-controlled container appropriate for access by the host and packet tunnel extension, with restrictive permissions.

## Tunnel settings

The extension applies DNS, routes, addresses, and MTU produced by the VPN engine/configuration only after the profile passed KMP validation and signature verification.

## Signing/provisioning

CI must support:
- Apple signing identities
- NetworkExtension entitlements
- app + extension bundle identifiers
- notarization path for release builds

Development builds may use local signing, but production acceptance requires the real entitlement path.

## Failure taxonomy

Keep errors separate:
- enrollment
- profile fetch
- cryptographic verification
- profile policy
- NetworkExtension configuration
- native OpenVPN engine
- network connectivity

## Open Questions

None blocking architecture; Apple developer entitlement availability is an external delivery dependency and must be verified before production release.
