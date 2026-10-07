# Design — Phase 00

## Context

This phase deliberately does not implement VPN connectivity. Its purpose is to constrain the architecture early enough that later Android/macOS tunnel integration cannot leak platform concerns into the KMP core.

## Repository structure

Use Gradle Kotlin DSL and a version catalog.

```text
apps/android
apps/macos
backend/registry-server
backend/admin-cli
shared/api-model
shared/domain
shared/profile
shared/profile-policy
shared/enrollment
shared/sync
shared/crypto
shared/persistence
shared/telemetry
native/openvpn-bridge
infra
openspec
```

## Source-set strategy

- `commonMain`: pure shared contracts and algorithms.
- `commonTest`: deterministic contract and state-machine tests.
- `jvmMain`: backend-only integration points.
- `androidMain`: Android secure storage and tunnel bindings.
- `macosMain`: Kotlin/Native adapters exported to Swift.
- Native OpenVPN implementation remains outside common source sets.

## Compatibility convention

Every externally persisted or transported payload has:
- `schemaVersion`
- stable identifiers
- forward-compatible unknown-field handling where safe
- explicit failure on unknown security-critical semantics

## Native bridge rule

Expose a small C ABI from `native/openvpn-bridge`; do not expose the OpenVPN C++ object model directly to Kotlin or Swift.

## CI

Minimum jobs:
1. shared JVM compile/test
2. Android compile/test
3. macOS/Kotlin Native framework compile on macOS runner
4. lint/static analysis
5. secret scan
6. OpenSpec validation

## Decisions

- Backend runtime: JVM.
- KMP is used for shared product logic, not as a requirement that every UI and OS integration be Kotlin.
- macOS shell is SwiftUI.
- Native tunnel engine integration is deferred to phases 03–04.

## Open Questions

None blocking implementation.
