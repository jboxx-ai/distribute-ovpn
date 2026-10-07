# Design — Phase 03

## Android composition

```text
Compose UI
   ↓
Android presentation/ViewModel
   ↓
KMP enrollment + sync engine
   ↓
TunnelController interface
   ↓
AndroidTunnelController
   ↓
VpnService
   ↓
JNI/C bridge
   ↓
OpenVPN native core
```

## Local storage

Use separate stores:
- secure device credential store: Android Keystore-backed
- metadata database/file store: profile IDs, versions, state
- encrypted or app-private profile artifact storage

Never log inline `<key>`, credentials, or complete `.ovpn` content.

## VPN service

The service owns tunnel lifecycle beyond Activity lifecycle. UI observes state via a stable application-level state source.

## Native bridge

Keep JNI minimal:
- create engine
- validate/prepare config
- attach TUN descriptor
- start
- stop
- query state/error events

Native callbacks are converted into typed Kotlin events before entering shared state.

## MVP activation rule

Phase 03 supports:
1. first profile becomes active after verification
2. later manual sync stages the new version
3. explicit user action may activate/reconnect

Automated atomic activation and rollback are Phase 05.

## Test matrix

- API 28+ baseline chosen by project
- current Android release
- emulator for non-tunnel tests
- at least one physical device for real VPN tunnel acceptance
- airplane/network transition tests
- process recreation tests

## Open Questions

None blocking implementation.
