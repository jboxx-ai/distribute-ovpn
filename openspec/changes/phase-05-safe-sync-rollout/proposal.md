# Phase 05 — Safe Synchronization, Activation, Rollback, and Rollout

## Why

Cross-platform clients exist, but updating an active VPN profile can break connectivity. Updates therefore need atomic staging, health verification, rollback, release channels, and controlled rollout semantics.

## What Changes

- Implement atomic profile activation.
- Preserve last-known-good profile until new tunnel health is verified.
- Rollback automatically after activation failure.
- Add stable/beta channels and per-device assignment.
- Add staged rollout controls and pause behavior.
- Add periodic background update checks within platform constraints.

## Capabilities

### New Capabilities

- `atomic-profile-activation`
- `automatic-rollback`
- `release-channels`
- `staged-rollout`
- `background-sync`

## Impact

- Changes clients from manual MVP synchronization to production-safe managed updates.
- Introduces server-side rollout state and health feedback.
