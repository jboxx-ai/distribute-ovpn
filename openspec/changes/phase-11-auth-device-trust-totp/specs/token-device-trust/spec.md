# Token-Bound Device Trust Specification

## Purpose

Defines user-bound device session management, token expiration, instant remote revocation, and Policy A (Strict Online Verification) enforcement.

## ADDED Requirements

### Requirement: Policy A (Strict Online Verification)
The client application SHALL perform an authenticated online pre-flight verification against the registry before activating any VPN tunnel.

#### Scenario: Server reachable with active token
- **GIVEN** an enrolled client with an active, unexpired device token
- **WHEN** the user initiates VPN tunnel connection and the registry confirms valid status
- **THEN** the client SHALL proceed with tunnel activation

#### Scenario: Server unreachable or token expired/revoked
- **GIVEN** an enrolled client attempting tunnel activation
- **WHEN** the registry endpoint is unreachable or returns HTTP 401/403
- **THEN** the client SHALL immediately abort tunnel activation and display an authentication error

### Requirement: Instant Remote Device Revocation
When an administrator revokes a device in the registry, all subsequent requests from that device SHALL be immediately rejected.

#### Scenario: Revoked device sync attempt
- **GIVEN** a device marked as `REVOKED` in the registry database
- **WHEN** the device attempts profile synchronization or tunnel pre-flight check
- **THEN** the registry SHALL return HTTP 403 Forbidden and the client SHALL terminate any active tunnel session
