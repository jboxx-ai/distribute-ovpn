# Security Model

## Assets

- OpenVPN profile artifacts
- embedded VPN credentials/certificates
- device authentication credentials
- registry signing private key
- trusted signing public keys
- device/profile assignments
- audit history

## Primary rules

1. Never trust downloaded `.ovpn` bytes merely because HTTPS succeeded.
2. Never make one global bearer secret part of shipped clients.
3. Never expose profile private material in logs, telemetry, crash reporting, or audit logs.
4. Never replace the last-known-good profile before candidate verification.
5. Never allow arbitrary script/plugin execution from remotely distributed profiles.
6. Never treat `downloaded`, `verified`, `staged`, `active`, and `healthy` as the same state.
7. Never make control-plane availability a prerequisite for using an already verified local active profile, unless explicitly configured otherwise.

## Recommended key separation

- TLS key: terminates HTTPS
- device credential: authenticates one device
- profile signing key: authenticates profile artifacts
- admin credential: authorizes registry mutations

Compromise of one key class should not automatically imply compromise of every other trust boundary.
