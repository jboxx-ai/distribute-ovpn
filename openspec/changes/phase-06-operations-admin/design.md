# Design — Phase 06

## CLI

`vpnctl` talks to authenticated admin APIs; it does not connect directly to PostgreSQL.

Primary commands:

```text
vpnctl profile create
vpnctl profile upload
vpnctl profile promote
vpnctl profile rollback
vpnctl device enroll-token
vpnctl device list
vpnctl device assign
vpnctl device revoke
vpnctl rollout create
vpnctl rollout pause
vpnctl rollout status
vpnctl audit tail
```

## Audit events

Append-only table with:
- event ID
- server timestamp
- actor identity
- action
- target type/id
- result
- request/correlation ID
- redacted metadata

Do not store `.ovpn` bodies, client private keys, passwords, or bearer tokens.

## Metrics

At minimum:
- HTTP request count/latency
- authenticated device count
- profile sync success/failure
- signature/policy rejection count
- activation success/failure
- rollback success/failure
- rollout eligible/updated/failed
- DB pool and migration health

## Backup

Separate:
1. PostgreSQL backup
2. encrypted signing key backup
3. deployment configuration backup

Perform restore drills, not just backup creation.

## Open Questions

None blocking implementation.
