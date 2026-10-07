# Design — Phase 05

## Client state machine

```text
ACTIVE(vN)
   │ update available
   ▼
DOWNLOADING(vN+1)
   ▼
VERIFYING
   ▼
STAGED(vN+1)
   ▼
ACTIVATING
   ├── success + health pass ──► ACTIVE(vN+1)
   └── failure ────────────────► ROLLING_BACK
                                      │
                                      ├── success ─► ACTIVE(vN)
                                      └── failure ─► DEGRADED
```

Persist transition intent before destructive actions.

## Health verification

V1 health should verify:
- native tunnel reports connected
- expected tunnel interface/network settings exist
- a configurable network check succeeds through the VPN path

Avoid relying on one public third-party endpoint as a hard-coded dependency.

## Update policies

- `MANUAL`
- `NOTIFY`
- `AUTO_APPLY`

Policy belongs to device assignment, with a client-side safe default if absent.

## Rollout model

```text
Rollout
- profile_id
- channel
- candidate_version
- state: draft|active|paused|completed|cancelled
- percentage or explicit cohort
```

Deterministic device bucketing prevents a device from moving between percentage cohorts.

## Rollback

Client automatic rollback is independent of server rollback. Server rollback changes the published target. Client rollback restores its last-known-good local version after local activation failure.

## Background execution

Use WorkManager on Android and supported macOS scheduling mechanisms. Correctness cannot depend on exact scheduler timing.

## Open Questions

None blocking implementation.
