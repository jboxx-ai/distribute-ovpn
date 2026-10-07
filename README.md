# KMP VPN Profile Platform — End-to-End OpenSpec Plan

A phased implementation plan for a self-hosted VPN profile registry and first-party Android/macOS OpenVPN clients in one Kotlin Multiplatform monorepo.

## Product goal

Build a private, self-hosted control plane that lets enrolled devices keep a stable logical VPN profile while the underlying `.ovpn` configuration changes over time.

The server distributes signed, versioned OpenVPN profiles. Android and macOS clients synchronize, validate, stage, activate, reconnect, verify, and roll back profiles without requiring manual delete/re-import workflows.

The VPS is a **configuration/control plane only**. It does not need to run an OpenVPN client or carry tunneled user traffic.

## Architecture principle

KMP owns shared product logic:

- API models and serialization
- device enrollment domain
- profile registry contracts
- `.ovpn` parsing and validation
- profile signature verification
- synchronization state machine
- rollout and rollback rules
- client persistence abstractions
- telemetry/event contracts

Platform-native layers own OS VPN plumbing:

- Android: `VpnService` + native OpenVPN engine bridge
- macOS: `NetworkExtension` / `NEPacketTunnelProvider` + native OpenVPN engine bridge

The OpenVPN protocol implementation is not rewritten. A native OpenVPN core is wrapped behind a narrow C ABI and platform adapters.

## OpenSpec workflow

Every phase is an independent OpenSpec change:

```text
proposal.md
├── specs/<capability>/spec.md
├── design.md
└── tasks.md
```

Implementation starts only after the phase has proposal, specs, design, and tasks.

## Phase order

1. `phase-00-foundation` — monorepo, build graph, shared contracts, CI
2. `phase-01-profile-registry` — versioned profile registry and API
3. `phase-02-device-trust` — enrollment, device identity, artifact signing
4. `phase-03-android-client` — Android sync + tunnel MVP
5. `phase-04-macos-client` — macOS sync + NetworkExtension tunnel
6. `phase-05-safe-sync-rollout` — atomic activation, rollback, channels, staged rollout
7. `phase-06-operations-admin` — CLI, audit, observability, backup/restore
8. `phase-07-production-hardening` — threat-model closure, release engineering, upgrade guarantees

## Delivery milestones

### Milestone A — Registry usable
Phases 00–02 complete. Profiles can be uploaded, versioned, signed, and fetched by authenticated enrolled devices.

### Milestone B — Android MVP
Phase 03 complete. An Android device can enroll, sync a profile, connect, reconnect after an update, and report status.

### Milestone C — Cross-platform MVP
Phase 04 complete. The same logical profile works on Android and macOS.

### Milestone D — Safe distribution
Phase 05 complete. Updates are atomic, resumable, rollback-capable, and support stable/beta channels.

### Milestone E — Operable production system
Phases 06–07 complete. Administration, audit, backups, observability, security hardening, and release gates are in place.

## Non-goals for V1

- Implementing a new VPN protocol
- WireGuard support
- Multi-tenant SaaS
- Enterprise SSO
- iOS support
- Windows support
- traffic proxying through the registry server
- remote arbitrary script execution
- generic MDM functionality
- in-app profile editing beyond safe metadata
