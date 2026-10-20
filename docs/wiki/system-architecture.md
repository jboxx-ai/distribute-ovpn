# System Architecture & Monorepo Topology

## 1. High-Level Architecture

The platform separates the **control plane** (responsible for policy validation, versioning, cryptographic signing, and device trust) from the **data plane** (which carries client OpenVPN tunnel traffic directly to OpenVPN gateways).

```text
                               ┌─────────────────────────────────────────┐
                               │       Profile Registry Server           │
                               │   - Ktor 3.1 Server Engine (Netty)      │
                               │   - Exposed ORM + HikariCP Connection   │
                               │   - PostgreSQL Database                 │
                               │   - Ed25519 Cryptographic Signer        │
                               └─────────────┬─────────────┬─────────────┘
                                             │             │
                             HTTPS Enrollment│             │HTTPS Sync & Manifest
                                             ▼             ▼
                     ┌──────────────────────────┐       ┌──────────────────────────┐
                     │    Android Client App    │       │     macOS Client App     │
                     │  - Android VpnService    │       │  - NetworkExtension      │
                     │  - Jetpack Compose UI    │       │  - SwiftUI Interface     │
                     │  - Shared KMP Domain     │       │  - VpnCore.framework K/N │
                     │  - Activation Journal    │       │  - Activation Journal    │
                     └─────────────┬────────────┘       └─────────────┬────────────┘
                                   │                                  │
                                   │ OpenVPN Protocol (UDP/TCP)       │ OpenVPN Protocol (UDP/TCP)
                                   ▼                                  ▼
                     ┌─────────────────────────────────────────────────────────────┐
                     │              Customer OpenVPN Gateways                      │
                     │         (Autonomous Data Plane / User Traffic)              │
                     └─────────────────────────────────────────────────────────────┘
```

---

## 2. Monorepo Structural Blueprint

```text
distribute-ovpn/
├── backend/
│   ├── registry-server/               # Ktor HTTP REST Control Plane
│   └── admin-cli/                     # vpnctl administrative CLI
├── shared/
│   ├── api-model/                     # Shared DTOs, ApiEnvelope, ReleaseChannel
│   ├── domain/                        # Core business interfaces (TunnelController, Storage)
│   ├── crypto/                        # Ed25519 signer & SHA-256 calculators
│   ├── profile-policy/                # .ovpn multiplatform AST parser & validator
│   ├── persistence/                   # Memory and filesystem storage adapters
│   ├── telemetry/                     # Event sinks and structured metrics
│   ├── sync/                          # SafeActivationEngine & Journal state machine
│   └── shared-umbrella/               # Kotlin/Native framework for Apple targets
├── apps/
│   ├── android/                       # Android application & VpnService
│   └── macos/                         # macOS SwiftUI app & NEPacketTunnelProvider
├── infra/
│   └── docker/                        # Multi-stage Dockerfile and Compose setup
├── docs/
│   └── wiki/                          # Worldwide enterprise documentation suite
└── openspec/                          # OpenSpec phase specifications (Phases 00–07)
```

---

## 3. Boundary Rules & Dependency Constraints

1. **Shared Core Independence**: Modules under `shared/*` MUST NEVER depend on `apps/*` or `backend/*`.
2. **Platform Abstraction**: OS-level subsystems (Android Keystore, macOS Keychain, NetworkExtension, VpnService) are abstracted behind strict shared interfaces (`SecureStorage`, `TunnelController`).
3. **Control Plane Isolation**: The VPS control plane does not route user payload traffic; compromising the registry server does not allow decrypting user tunnel streams.
