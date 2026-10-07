# Phase 07 — Production Hardening and Release Engineering

## Why

Before treating the platform as a durable security-sensitive system, threat-model findings, compatibility guarantees, release signing, upgrade safety, dependency provenance, and disaster scenarios must be closed.

## What Changes

- Complete threat-model-driven security hardening.
- Add upgrade and backward-compatibility gates.
- Add signed client release pipelines.
- Add SBOM and dependency provenance checks.
- Test compromised-profile, expired-credential, revoked-device, key-rotation, and registry-outage scenarios.
- Define production SLOs and release readiness checklist.

## Capabilities

### New Capabilities

- `security-hardening`
- `upgrade-compatibility`
- `release-integrity`
- `resilience`

## Impact

- Promotes the project from functional cross-platform MVP to production-ready personal/self-hosted infrastructure.
- May introduce breaking internal changes, but externally documented compatibility behavior must remain satisfied.
