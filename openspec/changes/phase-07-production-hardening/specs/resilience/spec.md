# Resilience Specification

## Purpose

Defines system behavior during control-plane and update failures.

## ADDED Requirements

### Requirement: Registry outage
A temporary registry outage SHALL NOT invalidate a previously verified active VPN profile on a device.

#### Scenario: Connect during registry outage

- **GIVEN** a previously verified usable active profile is stored locally

- **WHEN** the registry is unavailable
- **THEN** the client SHALL still be able to connect using that profile unless an explicit local expiry policy forbids it


### Requirement: Signing key rotation
The platform SHALL support signing-key rotation without requiring destructive device re-enrollment when a trusted transition path exists.

#### Scenario: Planned rotation

- **WHEN** a new verification key is introduced through an authenticated trusted transition
- **THEN** clients SHALL accept subsequently signed artifacts from the new key
