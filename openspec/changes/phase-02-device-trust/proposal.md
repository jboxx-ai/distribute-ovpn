# Phase 02 — Device Enrollment, Authentication, and Signing

## Why

A profile registry becomes a remote configuration authority. Devices therefore need scoped identities and clients must cryptographically verify every profile artifact independently of TLS.

## What Changes

- Add one-time device enrollment.
- Issue revocable per-device credentials.
- Assign devices to logical profiles and channels.
- Sign published profile versions using asymmetric signatures.
- Require clients to verify digest and signature before staging an artifact.
- Add device revocation and credential rotation behavior.

## Capabilities

### New Capabilities

- `device-enrollment`
- `device-authentication`
- `profile-signing`
- `device-assignment`

## Impact

- Changes profile retrieval from bootstrap-only access to authenticated device access.
- Introduces signing-key operational requirements.
