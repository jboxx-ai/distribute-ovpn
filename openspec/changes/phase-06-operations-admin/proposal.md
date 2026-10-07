# Phase 06 — Administration, Audit, Observability, and Recovery

## Why

A managed VPN platform is not production-operable without controlled administrative actions, forensic auditability, measurable health, and tested backup/restore procedures.

## What Changes

- Add `vpnctl` administration CLI.
- Add immutable audit events for security-sensitive operations.
- Add structured logs and Prometheus-compatible metrics.
- Add health/readiness endpoints.
- Add database and signing-material backup procedures.
- Add tested restore and disaster-recovery runbooks.

## Capabilities

### New Capabilities

- `admin-cli`
- `audit-log`
- `observability`
- `backup-restore`

## Impact

- Makes routine operation possible without direct database manipulation.
- Introduces retention and backup responsibilities.
