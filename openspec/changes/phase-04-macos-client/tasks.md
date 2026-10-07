# Tasks

## 1. macOS application shell

- [ ] 1.1 Create SwiftUI host application and import generated KMP framework.
- [ ] 1.2 Bind shared enrollment/sync state to SwiftUI presentation.
- [ ] 1.3 Implement Keychain-backed device credential adapter.

## 2. Profile persistence

- [ ] 2.1 Implement shared-container profile artifact storage.
- [ ] 2.2 Implement active/staged metadata persistence consistent with Android semantics.
- [ ] 2.3 Add macOS synchronization tests using fake registry and signature vectors.

## 3. NetworkExtension

- [ ] 3.1 Create PacketTunnel extension target and required entitlements.
- [ ] 3.2 Implement host-to-extension configuration handoff.
- [ ] 3.3 Integrate native OpenVPN bridge and tunnel lifecycle.

## 4. Release plumbing

- [ ] 4.1 Configure development signing for host app and extension.
- [ ] 4.2 Document production signing/notarization requirements.
- [ ] 4.3 Add macOS CI build on a signed-capable runner.

## 5. Acceptance

- [ ] 5.1 Run real Mac connect/disconnect test against controlled OpenVPN server.
- [ ] 5.2 Verify profile sync and tunnel failures remain independently diagnosable.
