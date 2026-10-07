# Tasks

## 1. Android application

- [x] 1.1 Create Compose application shell and dependency injection wiring.
- [x] 1.2 Implement enrolled/unenrolled and connected/disconnected UI states.
- [x] 1.3 Add secure Android device credential storage.

## 2. Synchronization

- [x] 2.1 Wire shared enrollment and sync engines into Android.
- [x] 2.2 Persist active and staged profile metadata and artifact files.
- [x] 2.3 Implement Sync Profile action and error presentation.

## 3. Tunnel

- [x] 3.1 Integrate Android VpnService and user permission flow.
- [x] 3.2 Build native OpenVPN bridge for Android ABI targets.
- [x] 3.3 Map tunnel lifecycle and native errors into typed Kotlin states.

## 4. Verification

- [x] 4.1 Add unit tests for sync state transitions.
- [x] 4.2 Add instrumentation tests for enrollment and storage.
- [x] 4.3 Run real-device connect/disconnect acceptance tests against a controlled OpenVPN server.

## 5. Status reporting

- [x] 5.1 Implement privacy-safe heartbeat/status reporting.
- [x] 5.2 Verify secrets and full profile contents are absent from telemetry and logs.
