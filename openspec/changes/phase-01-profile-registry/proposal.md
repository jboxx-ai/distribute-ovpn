# Phase 01 — Versioned Profile Registry

## Why

Clients need a stable source of truth for logical VPN profiles and immutable historical versions before device enrollment or synchronization can be trustworthy.

## What Changes

- Add Ktor/JVM registry service and PostgreSQL persistence.
- Support validated `.ovpn` profile upload and immutable version history.
- Publish a current version per logical profile.
- Expose versioned manifest and artifact retrieval APIs.
- Keep server-side publication state separate from device activation state.

## Capabilities

### New Capabilities

- `profile-registry`
- `profile-publication-api`
- `ovpn-ingestion`

## Impact

- Introduces the first deployable backend service.
- Does not yet permit unauthenticated production device use; device trust arrives in Phase 02.
