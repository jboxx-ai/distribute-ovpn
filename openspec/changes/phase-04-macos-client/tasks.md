# Tasks

## 1. macOS application shell

- [x] 1.1 Create SwiftUI host application and import generated KMP framework.
- [x] 1.2 Bind shared enrollment/sync state to SwiftUI presentation.
- [x] 1.3 Implement Keychain-backed device credential adapter.

## 2. Profile persistence

- [x] 2.1 Implement shared-container profile artifact storage.
- [x] 2.2 Implement active/staged metadata persistence consistent with Android semantics.
- [x] 2.3 Add macOS synchronization tests using fake registry and signature vectors.

## 3. NetworkExtension

- [x] 3.1 Create PacketTunnel extension target and required entitlements.
- [x] 3.2 Implement host-to-extension configuration handoff.
- [x] 3.3 Integrate native OpenVPN bridge and tunnel lifecycle.

## 4. Release plumbing

- [x] 4.1 Configure development signing for host app and extension.
- [x] 4.2 Document production signing/notarization requirements.
- [x] 4.3 Add macOS CI build on a signed-capable runner.

## 5. Acceptance

- [x] 5.1 Run real Mac connect/disconnect test against controlled OpenVPN server.
- [x] 5.2 Verify profile sync and tunnel failures remain independently diagnosable.
