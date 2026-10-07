# System Architecture

## Context

The platform solves one operational problem: a user owns multiple devices and receives an OpenVPN profile whose contents may change. Re-importing profiles manually on every device is error-prone. The platform introduces a self-hosted registry and managed clients that synchronize the same logical profile automatically.

## High-level topology

```text
                         ┌─────────────────────────┐
                         │ Profile Registry        │
                         │ Ktor/JVM                │
                         │ PostgreSQL              │
                         │ Artifact signing        │
                         └────────────┬────────────┘
                                      │ HTTPS
                     ┌────────────────┴────────────────┐
                     │                                 │
              Android Client                      macOS Client
                     │                                 │
              KMP shared core                   KMP shared core
                     │                                 │
              Android VpnService              NetworkExtension
                     │                                 │
                     └──────── native OpenVPN core ────┘
```

## Monorepo layout

```text
vpn-platform/
├── apps/
│   ├── android/
│   └── macos/
├── backend/
│   ├── registry-server/
│   └── admin-cli/
├── shared/
│   ├── api-model/
│   ├── domain/
│   ├── enrollment/
│   ├── profile/
│   ├── profile-policy/
│   ├── sync/
│   ├── crypto/
│   ├── persistence/
│   └── telemetry/
├── native/
│   └── openvpn-bridge/
├── infra/
│   ├── docker/
│   ├── migrations/
│   └── caddy/
├── openspec/
└── docs/
```

## Dependency direction

```text
platform UI / platform VPN adapter
              ↓
          shared domain
        ↙      ↓       ↘
 profile   enrollment   sync
        ↘      ↓       ↙
         api-model
```

`shared` modules never depend on application modules.

Native VPN bindings are accessed through a narrow platform contract:

```kotlin
interface TunnelController {
    val state: StateFlow<TunnelState>

    suspend fun prepare(profile: ValidatedProfile): PrepareResult
    suspend fun connect(): ConnectResult
    suspend fun disconnect(reason: DisconnectReason)
}
```

## Backend stack

- Kotlin/JVM
- Ktor Server
- PostgreSQL
- Flyway or equivalent deterministic migrations
- Caddy or another reverse proxy for TLS
- Docker Compose for initial deployment
- structured JSON logs
- Prometheus-compatible metrics endpoint

## Client stack

### Shared
- Kotlin Multiplatform
- coroutines / Flow
- kotlinx.serialization
- Ktor Client
- explicit interfaces for secure storage, clock, file persistence, and tunnel control

### Android
- Jetpack Compose
- Android Keystore
- `VpnService`
- NDK/JNI bridge to native OpenVPN engine

### macOS
- SwiftUI shell
- Kotlin/Native framework for shared core
- Keychain
- `NEPacketTunnelProvider`
- C/Objective-C bridge to native OpenVPN engine

## Security boundaries

1. Registry authenticates devices.
2. Registry signs profile artifacts with an offline-capable asymmetric signing key.
3. Clients pin/ship the public verification key.
4. Profiles are parsed and validated before activation.
5. Dangerous OpenVPN directives are blocked by policy.
6. New profiles are staged; active profiles remain available until activation succeeds.
7. Device private credentials stay in OS secure storage.
8. Registry never receives user VPN traffic.

## Profile lifecycle

```text
UPLOADED
   ↓
VALIDATED
   ↓
SIGNED
   ↓
PUBLISHED
   ↓
DOWNLOADED
   ↓
VERIFIED
   ↓
STAGED
   ↓
ACTIVATING
  / \
 /   \
ACTIVE FAILED
        ↓
     ROLLBACK
```

Server-side publication status and device-side activation status are intentionally separate.
