# Design — Phase 07

## Threat model closure

Review at least:
- malicious registry operator credential
- stolen enrollment token
- stolen device credential
- compromised artifact storage
- compromised TLS termination
- signing key compromise
- malicious `.ovpn`
- downgrade/rollback attack
- replayed manifest
- local device compromise
- native engine vulnerability
- supply-chain compromise

Every accepted risk must be explicit.

## Downgrade protection

Client tracks highest successfully activated version per profile/channel unless the server issues a specifically authorized rollback instruction that is itself signed and policy-compliant.

## Release pipeline

Backend:
- reproducible container build
- dependency scan
- SBOM
- image digest recording

Android:
- signed release bundle/APK
- version code monotonicity
- native ABI verification

macOS:
- signed host and extension
- notarization
- hardened runtime where applicable

## Compatibility

Maintain a compatibility matrix in source control:
- server API version
- manifest schema
- minimum Android client
- minimum macOS client
- native OpenVPN engine version

## Production readiness review

A release cannot be labeled production-ready until:
- backup restore drill passes
- key recovery drill passes
- rollback drill passes
- bad-profile rollout drill passes
- registry outage test passes
- revoked-device test passes
- dependency/license review passes

## Open Questions

None blocking implementation. Legal review may still be required before commercial distribution of third-party VPN engine components.
